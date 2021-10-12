package com.changhong.sei.edm.ocr.service.impl;

import com.changhong.sei.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实现功能：专用于处理电子发票识别的类
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2021-08-15 18:18
 */
public class InvoicePdfExtractor {

    public static DefaultCharacterReaderServiceImpl.InvoiceVO extract(File file) {
        DefaultCharacterReaderServiceImpl.InvoiceVO invoice;
        try (PDDocument doc = PDDocument.load(file)) {
            PDPage firstPage = doc.getPage(0);
            int pageWidth = Math.round(firstPage.getCropBox().getWidth());
            // 读取完整的PDF内容
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            String fullText = textStripper.getText(doc);
            if (firstPage.getRotation() != 0) {
                // 若有旋转,则将高转为宽
                pageWidth = Math.round(firstPage.getCropBox().getHeight());
            }
            invoice = new DefaultCharacterReaderServiceImpl.InvoiceVO();
            // 字符串预处理.去除空格,中文字符替换等
            String allText = replace(fullText).replaceAll("（", "(").replaceAll("）", ")").replaceAll("￥", "¥");
            {
                String reg = "机器编号:(?<machineNumber>\\d{12})|发票代码:(?<code>\\d{12})|发票号码:(?<number>\\d{8})|:(?<date>\\d{4}年\\d{2}月\\d{2}日)|校验码:(?<checksum>\\d{20}|\\S{4,})";
                Pattern pattern = Pattern.compile(reg);
                Matcher matcher = pattern.matcher(allText);
                while (matcher.find()) {
                    if (matcher.group("code") != null) {
                        invoice.setCode(matcher.group("code"));
                    } else if (matcher.group("number") != null) {
                        invoice.setNumber(matcher.group("number"));
                    } else if (matcher.group("date") != null) {
                        Date localDate = DateUtils.parseDate(matcher.group("date"), "yyyy年MM月dd日");
                        if (Objects.nonNull(localDate)) {
                            invoice.setDate(DateUtils.formatDate(localDate, DateUtils.FORMAT_YYYYMMDD));
                        }
                    } else if (matcher.group("checksum") != null) {
                        invoice.setCheckCode(matcher.group("checksum"));
                    }
                }
                if (StringUtils.isBlank(invoice.getCode())) {
                    reg = "发票(?<code>\\d{12})";
                    pattern = Pattern.compile(reg);
                    matcher = pattern.matcher(allText);
                    if (matcher.find()) {
                        String code = matcher.group("code");
                        // System.out.println(code);
                        invoice.setCode(code);
                    }
                }
            }
            {
                String reg = "价税合计\\u0028大写\\u0029(?<amountString>\\S*)\\u0028小写\\u0029¥?(?<amount>\\S*)\\s";
                Pattern pattern = Pattern.compile(reg);
                Matcher matcher = pattern.matcher(allText);
                if (matcher.find()) {
                    invoice.setTotalAmount(matcher.group("amount"));
                }
            }
            {
                String reg = "(?<p>\\S*)通发票";
                Pattern type00Pattern = Pattern.compile(reg);
                Matcher m00 = type00Pattern.matcher(allText);
                if (m00.find()) {
                    invoice.setCategory(m00.group());
                } else {
                    reg = "(?<p>\\S*)用发票";
                    Pattern type01Pattern = Pattern.compile(reg);
                    Matcher m01 = type01Pattern.matcher(allText);
                    if (m01.find()) {
                        invoice.setCategory(m01.group());
                    } else {
                        reg = "(?<p>\\S*)通用机打发票|(\\S*)通用\\u0028电子\\u0029发票";
                        Pattern type02Pattern = Pattern.compile(reg);
                        Matcher m02 = type02Pattern.matcher(allText);
                        if (m02.find()) {
                            invoice.setCategory(m02.group());
                        }
                    }
                }
            }
            PdfKeyWordPosition kwp = new PdfKeyWordPosition();
            Map<String, List<Position>> positionListMap = kwp
                    .getCoordinate(Arrays.asList("机器编号", "校验码", "税率", "价税合计", "合计", "开票日期", "规格型号", "车牌号", "开户行及账号", "密", "码", "区", "备", "注", "开票人"), doc);

            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);
            PDFTextStripperByArea detailStripper = new PDFTextStripperByArea();
            detailStripper.setSortByPosition(true);
            {
                Position machineNumber;
                if (positionListMap.get("机器编号").size() > 0) {
                    machineNumber = positionListMap.get("机器编号").get(0);
                } else if (positionListMap.get("校验码").size() > 0) {
                    machineNumber = positionListMap.get("校验码").get(0);
                } else {
                    machineNumber = positionListMap.get("开票日期").get(0);
                    machineNumber.setY(machineNumber.getY() + 10);
                }
                Position taxRate = positionListMap.get("税率").get(0);
                Position totalAmount = positionListMap.get("价税合计").get(0);
                Position amount = positionListMap.get("合计").get(0);
                Position drawer = positionListMap.get("开票人").get(0);
                Position model;
                if (!positionListMap.get("规格型号").isEmpty()) {
                    model = positionListMap.get("规格型号").get(0);
                } else {
                    if (!positionListMap.get("车牌号").isEmpty()) {
                        model = positionListMap.get("车牌号").get(0);
                        model.setX(model.getX() - 15);
                    } else {
                        model = positionListMap.get("税率").get(0);
                        model.setX(model.getX() - 300);
                    }
                }

                List<Position> account = positionListMap.get("开户行及账号");
                Position buyer;
                Position seller;
                if (account.size() < 2) {
                    buyer = new Position(51, 122);
                    seller = new Position(51, 341);
                } else {
                    buyer = account.get(0);
                    seller = account.get(1);
                }

                int maqX = 370;
                List<Position> mi = positionListMap.get("密");
                List<Position> ma = positionListMap.get("码");
                List<Position> qu = positionListMap.get("区");
                for (Position position : mi) {
                    float x1 = position.getX();
                    for (Position value : ma) {
                        float x2 = value.getX();
                        if (Math.abs(x1 - x2) < 5) {
                            for (Position item : qu) {
                                float x3 = item.getX();
                                if (Math.abs(x2 - x3) < 5) {
                                    maqX = Math.round((x1 + x2 + x3) / 3);
                                }
                            }
                        }
                    }
                }
                int remarkX = 370;
                List<Position> bei = positionListMap.get("备");
                List<Position> zhu = positionListMap.get("注");
                for (Position position : bei) {
                    float x1 = position.getX();
                    for (Position value : zhu) {
                        float x2 = value.getX();
                        if (Math.abs(x1 - x2) < 5) {
                            remarkX = Math.round((x1 + x2) / 2);
                        }
                    }
                }
                {
                    int x = remarkX + 10;
                    // 价税合计的y坐标为参考
                    int y = Math.round(totalAmount.getY()) + 10;
                    int w = pageWidth - remarkX - 10;
                    // 开票人的y为参考
                    int h = Math.round(drawer.getY()) - y - 10;
                    stripper.addRegion("remark", new Rectangle(x, y, w, h));
                }
                {
                    int x = Math.round(model.getX()) - 13;
                    // 用税率的y坐标作参考
                    int y = Math.round(taxRate.getY()) + 5;
                    // 价税合计的y坐标减去税率的y坐标
                    int h = Math.round(amount.getY()) - Math.round(taxRate.getY()) - 25;
                    detailStripper.addRegion("detail", new Rectangle(0, y, pageWidth, h));
                    stripper.addRegion("detailName", new Rectangle(0, y, x, h));
                    stripper.addRegion("detailPrice", new Rectangle(x, y, pageWidth, h));
                }
                {
                    int x = maqX + 10;
                    int y = Math.round(machineNumber.getY()) + 10;
                    int w = pageWidth - maqX - 10;
                    int h = Math.round(taxRate.getY() - 5) - y;
                    stripper.addRegion("password", new Rectangle(x, y, w, h));
                }
                {
                    // 开户行及账号的x为参考
                    int x = Math.round(buyer.getX()) - 15;
                    // 机器编号的y坐标为参考
                    int y = Math.round(machineNumber.getY()) + 10;
                    // 密码区x坐标为参考
                    int w = maqX - x - 5;
                    // 开户行及账号的y坐标为参考
                    int h = Math.round(buyer.getY()) - y + 20;
                    stripper.addRegion("buyer", new Rectangle(x, y, w, h));
                }
                {
                    // 开户行及账号为x参考
                    int x = Math.round(seller.getX()) - 15;
                    // 价税合计的y坐标为参考
                    int y = Math.round(totalAmount.getY()) + 10;
                    // 密码区的x为参考
                    int w = maqX - x - 5;
                    // 开户行及账号的y为参考
                    int h = Math.round(seller.getY()) - y + 20;
                    stripper.addRegion("seller", new Rectangle(x, y, w, h));
                }
            }
            stripper.extractRegions(firstPage);
            detailStripper.extractRegions(firstPage);

            String reg = "名称:(?<name>\\S*)";
            {
                String buyer = replace(stripper.getTextForRegion("buyer"));
                Pattern pattern = Pattern.compile(reg);
                Matcher matcher = pattern.matcher(buyer);
                if (matcher.find()) {
                    invoice.setBuyerName(matcher.group("name"));
                }
            }
            {
                String seller = replace(stripper.getTextForRegion("seller"));
                Pattern pattern = Pattern.compile(reg);
                Matcher matcher = pattern.matcher(seller);
                while (matcher.find()) {
                    if (matcher.group("name") != null) {
                        invoice.setSellerName(matcher.group("name"));
                    } else if (matcher.group("code") != null) {
                        invoice.setSellerTaxpayerId(matcher.group("code"));
                    }
                }
            }
            {
                // 金额合计
                BigDecimal totalAmount = BigDecimal.ZERO;
                // 税额合计
                BigDecimal taxAmount = BigDecimal.ZERO;
                String[] detailPriceStringArray = stripper.getTextForRegion("detailPrice")
                        .replaceAll("　", " ").replaceAll(" ", " ")
                        .replaceAll("\r", "").split("\\n");
                for (String detailString : detailPriceStringArray) {
                    String[] itemArray = StringUtils.split(detailString, " ");
                    if (2 == itemArray.length) {
                        totalAmount = totalAmount.add(new BigDecimal(itemArray[0]));
                        taxAmount = taxAmount.add(new BigDecimal(itemArray[1]));
                    } else if (6 == itemArray.length) {
                        totalAmount = totalAmount.add(new BigDecimal(itemArray[itemArray.length - 1]));
                        String taxRate = itemArray[itemArray.length - 3];
                        if (taxRate.indexOf("免税") > 0 || taxRate.indexOf("不征税") > 0 || taxRate.indexOf("出口零税率") > 0
                                || taxRate.indexOf("普通零税率") > 0) {
                            taxAmount = taxAmount.add(new BigDecimal(0));
                        } else {
                            taxAmount = taxAmount.add(new BigDecimal(itemArray[itemArray.length - 2]));
                        }
                    } else if (2 < itemArray.length) {
                        totalAmount = totalAmount.add(new BigDecimal(itemArray[itemArray.length - 3]));
                        String taxRate = itemArray[itemArray.length - 2];
                        if (taxRate.indexOf("免税") > 0 || taxRate.indexOf("不征税") > 0 || taxRate.indexOf("出口零税率") > 0
                                || taxRate.indexOf("普通零税率") > 0 || !taxRate.contains("%")) {
                            taxAmount = taxAmount.add(new BigDecimal(0));
                        } else {
                            BigDecimal rate = new BigDecimal(taxRate.replaceAll("%", ""));
                            taxAmount = taxAmount.add(rate.divide(new BigDecimal(100)));
                            totalAmount = totalAmount.add(new BigDecimal(itemArray[itemArray.length - 1]));
                        }
                    }
                }
                invoice.setAmount(totalAmount.subtract(taxAmount).toString());
                invoice.setTotalAmount(totalAmount.toString());
                invoice.setTaxAmount(taxAmount.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            invoice = null;
        }
        return invoice;
    }

    /**
     * 去除空格和中午冒号
     */
    public static String replace(String str) {
        return str.replaceAll(" ", "").replaceAll("　", "")
                .replaceAll("：", ":").replaceAll(" ", "");
    }
}
