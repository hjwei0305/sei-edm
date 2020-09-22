package com.changhong.sei.search.common.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.extractor.PowerPointExtractor;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xslf.extractor.XSLFPowerPointExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实现功能：文件内容识别工具类
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-09-22 15:15
 */
public class ReadFileContentUtil {

    private static final Pattern PATTERN = Pattern.compile("\\s*|\t|\r|\n");

    /**
     * 使用poi-scratchpad  读取doc文件内容
     *
     * @param fis 文件流
     * @return 文件内容
     */
    public static String readDocContent(InputStream fis) {
        String result = "";
        try {
            @SuppressWarnings("resource")
            WordExtractor wordExtractor = new WordExtractor(fis);
            result = wordExtractor.getText();
            result = replaceBlank(result);
            result = StringUtils.deleteWhitespace(result);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 使用poi-ooxml读取docx文件内容
     *
     * @param fis 文件流
     * @return 文件内容
     */
    public static String readDocxContent(InputStream fis) {
        String result = "";
        try {
            XWPFDocument xdoc = new XWPFDocument(fis);
            XWPFWordExtractor extractor = new XWPFWordExtractor(xdoc);
            result = extractor.getText();
            result = replaceBlank(result);
            result = StringUtils.deleteWhitespace(result);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 读取txt内容
     *
     * @param fis 文件流
     * @return 文件内容
     */
    public static String readTxtContent(InputStream fis) {
        String result = "";
        BytesEncodingDetectUtil bytesEncodingDetectUtil = new BytesEncodingDetectUtil();
        try {
            byte[] rawtext = bytesEncodingDetectUtil.getBytes(fis);
            String code = BytesEncodingDetectUtil.javaname[bytesEncodingDetectUtil.detectEncoding(rawtext)];
            result = new String(rawtext, code);
            result = replaceBlank(result);
            result = StringUtils.deleteWhitespace(result);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 读出的xls的内容
     *
     * @param fis 文件流
     * @return 文件内容
     */
    public static String readXlsContent(InputStream fis) {
        StringBuilder buff = new StringBuilder();
        String result = "";
        try {
            // 创建对Excel工作簿文件的引用
            HSSFWorkbook wb = new HSSFWorkbook(fis);
            // 创建对工作表的引用。
            for (int numSheets = 0; numSheets < wb.getNumberOfSheets(); numSheets++) {
                if (null != wb.getSheetAt(numSheets)) {
                    // 获得一个sheet
                    HSSFSheet aSheet = wb.getSheetAt(numSheets);
                    for (int rowNumOfSheet = 0; rowNumOfSheet <= aSheet
                            .getLastRowNum(); rowNumOfSheet++) {
                        if (null != aSheet.getRow(rowNumOfSheet)) {
                            // 获得一个行
                            HSSFRow aRow = aSheet.getRow(rowNumOfSheet);
                            for (int cellNumOfRow = 0; cellNumOfRow <= aRow
                                    .getLastCellNum(); cellNumOfRow++) {
                                if (null != aRow.getCell(cellNumOfRow)) {
                                    // 获得列值
                                    HSSFCell aCell = aRow.getCell(cellNumOfRow);
                                    switch (aCell.getCellTypeEnum()) {
                                        default:
                                            break;
                                        case NUMERIC:
                                            buff.append(aCell.getNumericCellValue()).append('\t');
                                            break;
                                        case STRING:
                                            buff.append(aCell.getStringCellValue()).append('\t');
                                            break;
                                    }
                                }
                            }
                            buff.append('\n');
                        }
                    }
                }
            }
            result = buff.toString();
            result = replaceBlank(result);
            result = StringUtils.deleteWhitespace(result);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 读出的xlsx的内容
     *
     * @param fis 文件流
     * @return 文件内容
     */
    public static String readXlsxContent(InputStream fis) {
        StringBuilder buff = new StringBuilder();
        String result = "";
        try {
            // 创建对Excel工作簿文件的引用
            XSSFWorkbook wb = new XSSFWorkbook(fis);
            // 创建对工作表的引用。
            for (int numSheets = 0; numSheets < wb.getNumberOfSheets(); numSheets++) {
                if (null != wb.getSheetAt(numSheets)) {
                    // 获得一个sheet
                    XSSFSheet aSheet = wb.getSheetAt(numSheets);
                    for (int rowNumOfSheet = 0; rowNumOfSheet <= aSheet.getLastRowNum(); rowNumOfSheet++) {
                        if (null != aSheet.getRow(rowNumOfSheet)) {
                            // 获得一个行
                            XSSFRow aRow = aSheet.getRow(rowNumOfSheet);
                            for (int cellNumOfRow = 0; cellNumOfRow <= aRow.getLastCellNum(); cellNumOfRow++) {
                                if (null != aRow.getCell(cellNumOfRow)) {
                                    // 获得列值
                                    XSSFCell aCell = aRow.getCell(cellNumOfRow);
                                    switch (aCell.getCellTypeEnum()) {
                                        case STRING:
                                            buff.append(aCell.getStringCellValue()).append('\t');
                                            break;
                                        case NUMERIC:
                                            buff.append(aCell.getNumericCellValue()).append('\t');
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                            buff.append('\n');
                        }
                    }
                }
            }
            result = buff.toString();
            result = replaceBlank(result);
            result = StringUtils.deleteWhitespace(result);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 使用pdfbox读取pdf文件内容
     *
     * @param fis 文件流
     * @return 文件内容
     */
    public static String readPdfContent(InputStream fis) {
        String result = "";
        PDDocument document = null;
        try {
            // 创建PDF解析器
            PDFParser parser = new PDFParser(new RandomAccessBuffer(fis));
            // 执行PDF解析过程
            parser.parse();
            // 获取解析器的PDF文档对象
            document = parser.getPDDocument();
            // 生成PDF文档内容剥离器
            PDFTextStripper stripper = new PDFTextStripper();
            // 利用剥离器获取文档
            result = stripper.getText(document);
            result = replaceBlank(result);
            result = StringUtils.deleteWhitespace(result);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 读取ppt文件内容
     *
     * @param is 文件流
     * @return 文件内容
     */
    public static String readPptContent(InputStream is) {
        String result = "";
        try {
            PowerPointExtractor extractor = new PowerPointExtractor(is);
            result = extractor.getText();
            result = replaceBlank(result);
            result = StringUtils.deleteWhitespace(result);
        } catch (IOException e) {
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

    /**
     * 读取pptx文件内容
     *
     * @param is 文件流
     * @return 文件内容
     */
    public static String readPptxContent(InputStream is) {
        String result = "";
        try {
            XMLSlideShow slideShow = new XMLSlideShow(is);
            result = new XSLFPowerPointExtractor(slideShow).getText();
            result = replaceBlank(result);
            result = StringUtils.deleteWhitespace(result);
        } catch (IOException e) {
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

    /**
     * 去除字符串中的所有回车、换行
     *
     * @param str 字符串
     * @return 处理结果
     */
    private static String replaceBlank(String str) {
        String dest = "";
        if (str != null) {
            Matcher matcher = PATTERN.matcher(str);
            dest = matcher.replaceAll("");
        }
        return dest;
    }
}
