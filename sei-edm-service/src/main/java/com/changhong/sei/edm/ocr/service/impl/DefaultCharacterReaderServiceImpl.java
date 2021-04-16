package com.changhong.sei.edm.ocr.service.impl;

import com.changhong.sei.core.dto.ResultData;
import com.changhong.sei.core.log.LogUtil;
import com.changhong.sei.core.util.JsonUtils;
import com.changhong.sei.edm.common.util.ImageUtils;
import com.changhong.sei.edm.common.util.ZxingUtils;
import com.changhong.sei.edm.dto.DocumentType;
import com.changhong.sei.edm.dto.OcrType;
import com.changhong.sei.edm.ocr.service.CharacterReaderService;
import com.changhong.sei.util.AmountUtils;
import com.changhong.sei.util.DateUtils;
import com.changhong.sei.util.FileUtils;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.QrcodeOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.QrcodeOCRResponse;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

// 导入对应产品模块的client
// 导入要请求接口对应的request response类

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-03-06 14:21
 */
@Component
public class DefaultCharacterReaderServiceImpl implements CharacterReaderService {

    /**
     * 识别条码匹配前缀
     */
    @Value("${sei.edm.ocr.match-prefix:sei}")
    private String matchStr;
    /**
     * tess data 安装目录
     */
    @Value("${sei.edm.ocr.tessdata-path:none}")
    private String tessDataPath;
    /**
     * 是否启用云识别
     */
    @Value("${sei.edm.ocr.cloud.enable:false}")
    private Boolean ocrCloudEnable;

    /**
     * 字符读取
     *
     * @param ocrType 识别类型
     * @param data    文件
     * @return 返回读取的内容
     */
    @Override
    public ResultData<String> read(DocumentType docType, OcrType ocrType, byte[] data) {
        String result = StringUtils.EMPTY;

        // 条码前缀匹配内容
        String[] matchPrefix = StringUtils.split(matchStr.toLowerCase(), ",");
        Set<String> resultSet = new HashSet<>();
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            switch (docType) {
                case Pdf:
                    // 解码PDF中的条码信息.实质是将pdf转为图片后再解码
                    try (PDDocument doc = PDDocument.load(inputStream)) {
                        result = doRecogonize(doc, 72, matchPrefix, ocrType, resultSet);
                        if (StringUtils.isBlank(result)) {
                            result = doRecogonize(doc, 2 * 72, matchPrefix, ocrType, resultSet);
                        }
                        if (StringUtils.isBlank(result)) {
                            result = doRecogonize(doc, 3 * 72, matchPrefix, ocrType, resultSet);
                        }
                    } catch (Exception e) {
                        LogUtil.error("the decode pdf may be not exit.");
                    }
                    break;
                case Image:
                    BufferedImage image = null;
                    try {
                        image = ImageIO.read(inputStream);
                        result = processImage(image, matchPrefix, ocrType, resultSet);
                    } catch (Exception e) {
                        LogUtil.error("the decode image may be not exit.", e);
//                } finally {
//                    if (image != null) {
//                        image.flush();
//                    }
                    }

                    break;
                default:
                    //throw new IllegalArgumentException("不支持的文件类型。");
            }
        } catch (IOException e) {
            LogUtil.error("文档识别异常", e);
        }

        if (!checkBarcode(result, matchPrefix, ocrType, resultSet)) {
            result = StringUtils.EMPTY;
        }
        if (StringUtils.isBlank(result) && resultSet.size() > 0) {
            result = resultSet.stream().filter(StringUtils::isNotBlank).findAny().orElse(StringUtils.EMPTY);
            resultSet.clear();
        }

        if (OcrType.InvoiceQr == ocrType) {
            String[] arr;
            if (StringUtils.isNotBlank(result)) {
                if (result.startsWith("https")) {
                    result = getBlockChainInvoice(result);
                } else {
                    arr = result.split("[,]");
                    if (arr.length >= 7) {
                        InvoiceVO invoiceVO = new InvoiceVO();
                        // 发票代码
                        String invoiceCode = arr[2];
                        invoiceVO.setCode(invoiceCode);
                        // 发票号码
                        invoiceVO.setNumber(arr[3]);
                        // 发票种类 01-增值税专用发票 04-增值税普通发票 10-增值税电子普通发票
                        if ("01".equals(arr[1])) {
                            invoiceVO.setCategory("增值税专用发票");
                        } else if ("04".equals(arr[1])) {
                            invoiceVO.setCategory("增值税普通发票");
                        } else if ("10".equals(arr[1])) {
                            invoiceVO.setCategory("增值税电子普通发票");
                        } else if ("20".equals(arr[1])) {
                            invoiceVO.setCategory("增值税电子专用发票");
                        } else {
                            invoiceVO.setCategory("通用机打发票(电子)");
                        }
                        String invoiceDate;
                        //增值税电子普通发票的发票代码最后一位数为“1”，而区块链电子发票的发票代码最后一位数为“0”
                        if (invoiceCode.endsWith("0") && arr[7].length() > 4) {
                            // 区块链发票：01,10,153002009100,00253776,92530828MA6P3AA16R,1100.00,20200815,003297b4988d0c0e12fce1584451b2d7cb4b8f169316243905c40338d6e7657add
                            // 开票金额(不含税) arr[5]
                            invoiceVO.setAmount(arr[5]);
                            // 开票日期 arr[6]，
                            invoiceDate = arr[6];
                            //二维码信息无校验码
                        } else {
                            // 普通发票：01,10,053001800111,30219862,800.00,20200728,07039881687041428131,7497
                            // 广东通用机打发票(电子): 01,20,144002009010,02703269,391.00,20201010,00000000002015724400,4F92
                            // 开票金额(不含税) arr[4]
                            invoiceVO.setAmount(arr[4]);
                            // 开票日期 arr[5]
                            invoiceDate = arr[5];
                            // 校验码
                            invoiceVO.setCheckCode(arr[6]);
                            if (arr.length > 7) {
                                //随机码
                                invoiceVO.setRandom(arr[7]);
                            }
                        }
                        // 开票日期截取日期
                        if (StringUtils.isNoneBlank(invoiceDate) && invoiceDate.length() > 8) {
                            invoiceDate = invoiceDate.substring(0, 8);
                        }
                        invoiceVO.setDate(invoiceDate);
                        result = JsonUtils.toJson(invoiceVO);
                    }
                    // 浙江通用电子发票
                    else if (arr.length == 5) {
                        InvoiceVO invoiceVO = new InvoiceVO();
                        // 发票代码
                        invoiceVO.setCode(arr[0]);
                        // 发票号码
                        invoiceVO.setNumber(arr[1]);
                        // 发票种类 01-增值税专用发票 04-增值税普通发票 10-增值税电子普通发票
                        //invoiceVO.setCategory("增值税专用发票");
                        //invoiceVO.setCategory("增值税普通发票");
                        invoiceVO.setCategory("增值税电子普通发票");
                        // 总金额（含税）价税合计
                        invoiceVO.setTotalAmount(arr[3]);
                        // 开票日期 arr[2]
                        String invoiceDate = arr[2];
                        if (StringUtils.isNoneBlank(invoiceDate) && invoiceDate.length() > 8) {
                            invoiceDate = invoiceDate.substring(0, 8);
                        }
                        invoiceVO.setDate(invoiceDate);
                        // 校验码
                        invoiceVO.setCheckCode(arr[4]);
                        result = JsonUtils.toJson(invoiceVO);
                    }
                }
            }
        }

        return ResultData.success(result);
    }

    /**
     * OCR识别
     */
    private String partImgOcr(BufferedImage bimg, final String[] matchPrefix) {
        String result = StringUtils.EMPTY;
        if (StringUtils.isBlank(tessDataPath) || StringUtils.equalsIgnoreCase("none", tessDataPath)) {
            return result;
        }

        try {
            //开始识别
            ITesseract instance = new Tesseract();
            // 解决Warning: Invalid resolution 0 dpi. Using 70 instead.
            instance.setTessVariable("user_defined_dpi", "300");
            //语言：英文
            instance.setLanguage("eng");
            //Tesseract的文字库
//        instance.setDatapath("/opt/tesseract-4.1.0/tessdata");
            //本地调试
//        instance.setDatapath("D:\\Program Files\\Tesseract-OCR\\tessdata");
            instance.setDatapath(tessDataPath);
            int pageIteratorLevel = ITessAPI.TessPageIteratorLevel.RIL_WORD;
            List<Word> words = instance.getWords(bimg, pageIteratorLevel);
            if (!CollectionUtils.isEmpty(words)) {
                //处理：去掉开头结尾的空白字符
                Word word = words.parallelStream().filter(i -> {
                    if (Objects.nonNull(i)) {
                        String text = i.getText();
                        if (StringUtils.isNotBlank(text)) {
                            text = text.trim();

                            // 如果已“NO.”开头，去掉“NO.”
                            if (text.toUpperCase().startsWith("NO.")) {
                                text = text.substring(3);
                            }
                            // 匹配指定前缀
                            return StringUtils.startsWithAny(text.toLowerCase(), matchPrefix);
                        }
                    }
                    return false;
                }).findAny().orElse(null);

                if (Objects.nonNull(word)) {
                    result = word.getText();
                    // 如果已“NO.”开头，去掉“NO.”
                    if (result.toUpperCase().startsWith("NO.")) {
                        result = result.substring(3);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String doRecogonize(PDDocument doc, float dpi, String[] matchPrefix, OcrType ocrType, Set<String> resultSet) {
        BufferedImage image = null;
        PDFRenderer renderer = new PDFRenderer(doc);
        try {
            image = renderer.renderImageWithDPI(0, dpi, ImageType.GRAY);

            return processImage(image, matchPrefix, ocrType, resultSet);
        } catch (Exception e) {
            LogUtil.error("the decode pdf may be not exit.", e);
            return null;
        } finally {
            if (image != null) {
                image.flush();
            }
        }
    }

    private String processImage(BufferedImage image, String[] matchPrefix, OcrType ocrType, Set<String> resultSet) {
        BufferedImage image1, image2;
        String result;
        // 原图识别
        if (Objects.equals(OcrType.Barcode, ocrType)) {
            result = ZxingUtils.processImageBarcode(image, matchPrefix);
        } else {
            result = ZxingUtils.processImageQr(image, matchPrefix);
        }
        if (!checkBarcode(result, matchPrefix, ocrType, resultSet)) {
            int height = image.getHeight();
            int width = image.getWidth();
            // 剪切右上角
            image1 = image.getSubimage(width / 2, 0, width / 2, height / 4);
            // 指定识别右上角
            if (Objects.equals(OcrType.Barcode, ocrType)) {
                result = ZxingUtils.processImageBarcode(image1, matchPrefix);

                // ocr识别
                if (!checkBarcode(result, matchPrefix, ocrType, resultSet)) {
                    //条码识别失败，进行ocr识别
                    result = partImgOcr(image1, matchPrefix);
                }
            } else {
                result = ZxingUtils.processImageQr(image1, matchPrefix);
            }
            image1 = null;
        }

        // 识别失败，原图片旋转270度再次识别
        if (!checkBarcode(result, matchPrefix, ocrType, resultSet)) {
            // 旋转270度
            image1 = ImageUtils.rotate(image, 270);
            int height = image1.getHeight();
            int width = image1.getWidth();

            // 剪切右上角
            image2 = image1.getSubimage(width / 2, 0, width / 2, height / 4);
//                        BufferedImage image2 = image.getSubimage(0, 0, width, height / 2);
            if (Objects.equals(OcrType.Barcode, ocrType)) {
                result = ZxingUtils.processImageBarcode(image2, matchPrefix);

                // ocr识别
                if (!checkBarcode(result, matchPrefix, ocrType, resultSet)) {
                    //条码识别失败，进行ocr识别
                    result = partImgOcr(image2, matchPrefix);
                }
            } else {
                result = ZxingUtils.processImageQr(image2, matchPrefix);
            }
            image1 = null;
            image2 = null;
        }

        // 识别失败，原图片旋转180度再次识别
        if (!checkBarcode(result, matchPrefix, ocrType, resultSet)) {
            // 旋转180度
            image1 = ImageUtils.rotate(image, 180);
            int height = image1.getHeight();
            int width = image1.getWidth();

            // 剪切右上角
            image2 = image1.getSubimage(width / 2, 0, width / 2, height / 4);
//                        BufferedImage image2 = image.getSubimage(0, 0, width, height / 2);
            if (Objects.equals(OcrType.Barcode, ocrType)) {
                result = ZxingUtils.processImageBarcode(image2, matchPrefix);

                // ocr识别
                if (!checkBarcode(result, matchPrefix, ocrType, resultSet)) {
                    //条码识别失败，进行ocr识别
                    result = partImgOcr(image2, matchPrefix);
                }
            } else {
                result = ZxingUtils.processImageQr(image2, matchPrefix);
            }
            image1 = null;
            image2 = null;
        }

        // 识别失败，原图片旋转90度再次识别
        if (!checkBarcode(result, matchPrefix, ocrType, resultSet)) {
            // 旋转90度
            image1 = ImageUtils.rotate(image, 90);
            int height = image1.getHeight();
            int width = image1.getWidth();

            // 剪切右上角
            image2 = image1.getSubimage(width / 2, 0, width / 2, height / 4);
//                        BufferedImage image2 = image.getSubimage(0, 0, width, height / 2);
            if (Objects.equals(OcrType.Barcode, ocrType)) {
                result = ZxingUtils.processImageBarcode(image2, matchPrefix);

                // ocr识别
                if (!checkBarcode(result, matchPrefix, ocrType, resultSet)) {
                    //条码识别失败，进行ocr识别
                    result = partImgOcr(image2, matchPrefix);
                }
            } else {
                result = ZxingUtils.processImageQr(image2, matchPrefix);
            }
            image1 = null;
            image2 = null;
        }

        // 识别失败，调用云服务识别
        if (!checkBarcode(result, matchPrefix, ocrType, resultSet)
                && ocrCloudEnable) {
            result = tencentQrcodeOCRApi(image);
        }

        return result;
    }

    private String tencentQrcodeOCRApi(BufferedImage image) {
        String result = "";
        InputStream is = null;
        try {
            is = ImageUtils.image2InputStream(image, "");
            result = tencentQrcodeOCRApi(FileUtils.stream2Str(is));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    private String tencentQrcodeOCRApi(String image) {
        String result = StringUtils.EMPTY;
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey
            Credential cred = new Credential("AKIDCAwSlnhAVTHuHUp960CgusStT0a0LVs0 ", "tVyMZZJoIekZlhfMn8mBKEsaDZeYsxmR");

            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("ocr.tencentcloudapi.com");

            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            OcrClient client = new OcrClient(cred, "ap-guangzhou", clientProfile);

            StringBuilder params = new StringBuilder();
            params.append("{\"ImageBase64\":\"data:image/png;base64,").append(image).append("\"}");
            QrcodeOCRRequest req = QrcodeOCRRequest.fromJsonString(params.toString(), QrcodeOCRRequest.class);
            params = null;
            // 通过client对象调用想要访问的接口，需要传入请求对象
            QrcodeOCRResponse resp = client.QrcodeOCR(req);
            // 输出json格式的字符串回包
            LogUtil.debug("调用腾讯识别服务响应结果: {}", QrcodeOCRRequest.toJsonString(resp));
            if (ArrayUtils.isNotEmpty(resp.getCodeResults())) {
                result = resp.getCodeResults()[0].getUrl();
            }
        } catch (TencentCloudSDKException e) {
            LogUtil.error("调用腾讯识别服务异常", e);
        }
        return result;
    }

    /**
     * 检查识别内容
     */
    private boolean checkBarcode(String data, String[] matchPrefix, OcrType ocrType, Set<String> resultSet) {
        if (StringUtils.isBlank(data)) {
            return false;
        } else {
            resultSet.add(data);
        }
        // 是否是条码
        if (OcrType.Barcode == ocrType) {
            // 前缀匹配
            if (matchPrefix != null && matchPrefix.length > 0
                    && !StringUtils.startsWithAny(data.toLowerCase(), matchPrefix)) {
                return false;
            }
        }
        return true;
    }

    static final String BLOCK_CHAIN_INVOICE_HOST = "bcfp.shenzhen.chinatax.gov.cn";

    private String getBlockChainInvoice(String invoiceStr) {
        //https://bcfp.shenzhen.chinatax.gov.cn/verify/scan?hash=008c9dd9fb50e876a80ca41a767f54664d8c62771c1499cf6420869c88d24759e7&bill_num=05264260&total_amount=30000
        try {
            URI uri = new URI(invoiceStr);
            if (!BLOCK_CHAIN_INVOICE_HOST.equalsIgnoreCase(uri.getHost())) {
                return null;
            }

            HashMap<String, String> paraMap = new HashMap<>();
            String[] paraArr = uri.getQuery().split("&");
            for (String item : paraArr) {
                int index = item.indexOf("=");
                if (index == -1) {
                    continue;
                }
                String paraName = item.substring(0, index).toLowerCase();
                String paraValue = item.substring(index + 1);

                paraMap.put(paraName, paraValue);
            }

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> postParameters = new HashMap<>();
            postParameters.put("bill_num", paraMap.get("bill_num"));
            postParameters.put("total_amount", paraMap.get("total_amount"));
            postParameters.put("tx_hash", paraMap.get("hash"));
            HttpEntity<String> entity = new HttpEntity<>(JsonUtils.toJson(postParameters), headers);

            String url = uri.getScheme() + "://" + uri.getHost() + "/dzswj/bers_ep_web/query_bill_detail";
            ResponseEntity<HashMap> response = restTemplate.exchange(url, HttpMethod.POST, entity, HashMap.class);
            if (200 != response.getStatusCode().value()) {
                return null;
            }

            HashMap<String, Object> billRecord = (HashMap<String, Object>) response.getBody().get("bill_record");

            // billRecord.get("") 拿出想要的数据，这里的金额需要注意一下
            // 取出来的具体值需要 * 0.01，才是我们需要的值
            // bill_code
            // bill_num
            // amount：不含税总金额
            // tax_amount：税额
            // total_amount：总金额（含税）
            // time
            // seller_name
            // seller_taxpayer_id
            // buyer_name
            InvoiceVO invoiceVO = new InvoiceVO();
            // 发票代码
            invoiceVO.setCode(String.valueOf(billRecord.get("bill_code")));
            // 发票号码
            invoiceVO.setNumber(String.valueOf(billRecord.get("bill_num")));
            invoiceVO.setCategory("区块链电子发票");
            // 购买方名称
            invoiceVO.setBuyerName(String.valueOf(billRecord.get("buyer_name")));
            // 销货方名称
            invoiceVO.setSellerName(String.valueOf(billRecord.get("seller_name")));
            invoiceVO.setSellerTaxpayerId(String.valueOf(billRecord.get("seller_taxpayer_id")));
            // 开票金额(不含税) arr[4]
            invoiceVO.setAmount(AmountUtils.changeF2Y(Double.parseDouble(billRecord.get("amount").toString())).toString());
            // 税额
            invoiceVO.setTaxAmount(AmountUtils.changeF2Y(Double.parseDouble(billRecord.get("tax_amount").toString())).toString());
            // 总金额（含税）价税合计
            invoiceVO.setTotalAmount(AmountUtils.changeF2Y(Double.parseDouble(billRecord.get("total_amount").toString())).toString());
            // 开票日期 arr[5]
            Date date = new Date(Long.parseLong(billRecord.get("time").toString()) * 1000L);
            invoiceVO.setDate(DateUtils.formatDate(date));
            // 校验码
            invoiceVO.setCheckCode(String.valueOf(billRecord.get("tx_hash")));

            return JsonUtils.toJson(invoiceVO);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    static class InvoiceVO implements Serializable {
        private static final long serialVersionUID = 8754280672814795379L;
        // 发票代码
        private String code;
        // 发票号码
        private String number;
        // 发票类型
        private String category;
        // 购买方名称
        private String buyerName;
        // 销货方名称
        private String sellerName;
        private String sellerTaxpayerId;
        // 开票金额(不含税)
        private String amount;
        // 税额
        private String taxAmount;
        // 总金额（含税）价税合计
        private String totalAmount;
        // 开票日期
        private String date;
        // 校验码
        private String checkCode;
        private String random;
        // 发票状态
        private String status;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getBuyerName() {
            return buyerName;
        }

        public void setBuyerName(String buyerName) {
            this.buyerName = buyerName;
        }

        public String getSellerName() {
            return sellerName;
        }

        public void setSellerName(String sellerName) {
            this.sellerName = sellerName;
        }

        public String getSellerTaxpayerId() {
            return sellerTaxpayerId;
        }

        public void setSellerTaxpayerId(String sellerTaxpayerId) {
            this.sellerTaxpayerId = sellerTaxpayerId;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getTaxAmount() {
            return taxAmount;
        }

        public void setTaxAmount(String taxAmount) {
            this.taxAmount = taxAmount;
        }

        public String getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(String totalAmount) {
            this.totalAmount = totalAmount;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getCheckCode() {
            return checkCode;
        }

        public void setCheckCode(String checkCode) {
            this.checkCode = checkCode;
        }

        public String getRandom() {
            return random;
        }

        public void setRandom(String random) {
            this.random = random;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
