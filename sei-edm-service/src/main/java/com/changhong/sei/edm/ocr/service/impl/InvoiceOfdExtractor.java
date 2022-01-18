package com.changhong.sei.edm.ocr.service.impl;

import com.changhong.sei.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2021-08-14 06:45
 */
public final class InvoiceOfdExtractor {
    public static final String INVOICE_FILE = "original_invoice";
    public static final String CONTENT_FILE = "Content";

    /**
     * 构造方法.
     */
    private InvoiceOfdExtractor() {

    }

    public static void main(final String[] args) {
        // 1.解压ofd文件
        try {
            File ofdFile = new File("/Users/chaoma/Downloads/123.ofd");
            Map<String, String> xmlPathMap = unzip(ofdFile);
            final SAXReader reader = new SAXReader();
            Document doc;
            try {
                xmlPathMap = new HashMap<>();
                String xmlPath = xmlPathMap.get(INVOICE_FILE);
                doc = reader.read(new File(xmlPath));
                // 获取document对象根节点，即最外层节点下的内容
                final Element rootElement = doc.getRootElement();
                // 发票代码、发票号码、
                DefaultCharacterReaderServiceImpl.InvoiceVO invoice = new DefaultCharacterReaderServiceImpl.InvoiceVO();
                generateInvoice(rootElement, invoice);
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ofd发票文件解析.
     *
     * @param file file
     * @return Invoice
     */
    public static DefaultCharacterReaderServiceImpl.InvoiceVO parseOfd(final File file) {
        // 发票代码、发票号码、
        DefaultCharacterReaderServiceImpl.InvoiceVO invoice;
        try {
            invoice = new DefaultCharacterReaderServiceImpl.InvoiceVO();
            final Map<String, String> xmlPathMap = unzip(file);
            final SAXReader reader = new SAXReader();
            Document doc;
            String xmlPath = xmlPathMap.get(INVOICE_FILE);
            if (StringUtils.isNotBlank(xmlPath)) {
                doc = reader.read(new File(xmlPath));
                // 获取document对象根节点，即最外层节点下的内容
                final Element rootElement = doc.getRootElement();

                generateInvoice(rootElement, invoice);
            }

            xmlPath = xmlPathMap.get(CONTENT_FILE);
            if (StringUtils.isNotBlank(xmlPath)) {
                doc = reader.read(new File(xmlPath));
                // 获取document对象根节点，即最外层节点下的内容
                final Element rootElement = doc.getRootElement();
                String allText = rootElement.getStringValue();
                String reg = "(\\S*)通发票";
                Pattern typePattern = Pattern.compile(reg);
                Matcher m0 = typePattern.matcher(allText);
                if (m0.find()) {
                    invoice.setCategory(getInvoiceType(m0.group()));
                } else {
                    reg = "(\\S*)用发票";
                    typePattern = Pattern.compile(reg);
                    m0 = typePattern.matcher(allText);
                    if (m0.find()) {
                        invoice.setCategory(getInvoiceType(m0.group()));
                    }
                }
            }
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
            invoice = null;
        }
        return invoice;
    }

    private static String getInvoiceType(String type) {
        if (StringUtils.contains(type, "专用")) {
            if (StringUtils.contains(type, "电")) {
                return "增值税电子专用发票";
            } else {
                return "增值税专用发票";
            }
        } else if (StringUtils.contains(type, "普通")) {
            if (StringUtils.contains(type, "电")) {
                return "增值税电子普通发票";
            } else {
                return "增值税普通发票";
            }
        } else if (StringUtils.contains(type, "通用")) {
            if (StringUtils.contains(type, "电")) {
                return "通用机打发票(电子)";
            } else {
                return "通用机打发票";
            }
        } else {
            return "增值税发票";
        }
    }

    /**
     * 生成发票信息.
     *
     * @param rootElement rootElement
     * @param invoice     invoice
     */
    private static void generateInvoice(final Element rootElement, final DefaultCharacterReaderServiceImpl.InvoiceVO invoice) {
        // 发票代码
        final Element invoiceCodeElement = rootElement.element("InvoiceCode");
        // 发票号码
        final Element invoiceNoElement = rootElement.element("InvoiceNo");
        // 开票日期
        final Element issueDateElement = rootElement.element("IssueDate");
        // 校验码
        final Element invoiceCheckCodeElement = rootElement.element("InvoiceCheckCode");
        // 购买方
        final Element buyerElement = rootElement.element("Buyer");
        // 销售方
        final Element sellerElement = rootElement.element("Seller");
        // 税额
        final Element taxTotalAmountElement = rootElement.element("TaxTotalAmount");
        // 金额
        final Element taxExclusiveTotalAmountElement = rootElement.element("TaxExclusiveTotalAmount");
        // 价税合计
        final Element taxInclusiveTotalAmountElement = rootElement.element("TaxInclusiveTotalAmount");
        if (invoiceCodeElement != null) {
            invoice.setCode(invoiceCodeElement.getText());
        }
        if (invoiceNoElement != null) {
            invoice.setNumber(invoiceNoElement.getText());
        }
        if (issueDateElement != null && issueDateElement.getText() != null) {
            Date localDate = DateUtils.parseDate(issueDateElement.getText(), "yyyy年MM月dd日");
            if (Objects.nonNull(localDate)) {
                invoice.setDate(DateUtils.formatDate(localDate, DateUtils.FORMAT_YYYYMMDD));
            }
        }
        if (invoiceCheckCodeElement != null) {
            invoice.setCheckCode(invoiceCheckCodeElement.getText());
        }
        if (buyerElement != null) {
            final List<Element> buyerName = buyerElement.elements("BuyerName");
            invoice.setBuyerName(buyerName.get(0).getText());
        }
        if (sellerElement != null) {
            final List<Element> sellerName = sellerElement.elements("SellerName");
            final List<Element> sellerTaxId = sellerElement.elements("SellerTaxID");
            invoice.setSellerName(sellerName.get(0).getText());
            invoice.setSellerTaxpayerId(sellerTaxId.get(0).getText());
        }
        String temp;
        if (taxTotalAmountElement != null) {
            temp = taxTotalAmountElement.getText();
            invoice.setAmount(temp);
        }
        if (taxExclusiveTotalAmountElement != null) {
            temp = taxExclusiveTotalAmountElement.getText();
            invoice.setTotalAmount(temp);
        }
        if (taxInclusiveTotalAmountElement != null) {
            temp = taxInclusiveTotalAmountElement.getText();
            invoice.setTotalAmount(temp);
        }
    }

    /**
     * 从字节流写到文件中.
     *
     * @param is 流
     * @return file
     */
    private static File getFileByBytes(final InputStream is, final String fileName, final String oldFileName) {
        final String tempPath = System.getProperty("java.io.tmpdir");
        final File dirFile = new File(tempPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        final File file = new File(dirFile, fileName); // zip文件需临时存放服务器
        final File oldFile = new File(dirFile, oldFileName);
        OutputStream os = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            } else {
                file.delete();
                file.createNewFile();
            }
            if (oldFile.exists()) {
                file.delete();
            }
            os = new FileOutputStream(file);
            int len;
            final byte[] buffer = new byte[4096];
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    /**
     * 解压ofd文件.
     *
     * @param ofdFile ofdFile
     * @return ofdFile路径
     * @throws IOException IOException
     */
    public static Map<String, String> unzip(final File ofdFile) throws IOException {
        Map<String, String> filePathMap = new HashMap<>();
        final ZipFile zipFile = new ZipFile(ofdFile);
        String lastFileName = "";
        String temp = "";
        // 循环查找文件
        final Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            if (!zipEntry.isDirectory()) {
                String fileFullName = zipEntry.getName();
                // 若当前文件包含文件夹名称，则直接去文件名称
                if (fileFullName.contains("/") && fileFullName.contains(INVOICE_FILE)) {
                    fileFullName = fileFullName.substring(fileFullName.lastIndexOf("/") + 1);
                    lastFileName = fileFullName;
                    final InputStream in = zipFile.getInputStream(zipEntry);
                    final File xmlfile = getFileByBytes(in, fileFullName, lastFileName);
                    temp = xmlfile.toString();
                    filePathMap.put(INVOICE_FILE, temp);
                } else if (fileFullName.contains("/Page") && fileFullName.contains(CONTENT_FILE)) {
                    fileFullName = fileFullName.substring(fileFullName.lastIndexOf("/") + 1);
                    lastFileName = fileFullName;
                    final InputStream in = zipFile.getInputStream(zipEntry);
                    final File xmlfile = getFileByBytes(in, fileFullName, lastFileName);
                    temp = xmlfile.toString();
                    filePathMap.put(CONTENT_FILE, temp);
                }
            }
        }
        zipFile.close();
        return filePathMap;
    }
}
