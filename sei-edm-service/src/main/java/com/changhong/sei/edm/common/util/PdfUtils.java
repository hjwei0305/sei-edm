package com.changhong.sei.edm.common.util;

import com.changhong.sei.util.FileUtils;
import com.itextpdf.text.Font;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2019-10-11 13:44
 */
public class PdfUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfUtils.class);

    /**
     * @param bos           输出文件的位置
     * @param input         原PDF位置
     * @param waterMarkName 页脚添加水印
     */
    public static void setWatermark(BufferedOutputStream bos, String input, String waterMarkName)
            throws DocumentException, IOException {

        PdfReader reader = new PdfReader(input);
        PdfStamper stamper = new PdfStamper(reader, bos);

        // 获取总页数 +1, 下面从1开始遍历
        int total = reader.getNumberOfPages() + 1;
        // 使用classpath下面的字体库
        BaseFont base = null;
        try {
            base = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
        } catch (Exception e) {
            // 日志处理
            e.printStackTrace();
        }

        // 间隔
        int interval = -5;
        // 获取水印文字的高度和宽度
        int textH = 0, textW = 0;
        JLabel label = new JLabel();
        label.setText(waterMarkName);
        FontMetrics metrics = label.getFontMetrics(label.getFont());
        textH = metrics.getHeight();
        textW = metrics.stringWidth(label.getText());

        // 设置水印透明度
        PdfGState gs = new PdfGState();
        gs.setFillOpacity(0.3f);
        gs.setStrokeOpacity(0.3f);

        Rectangle pageSizeWithRotation = null;
        PdfContentByte content = null;
        for (int i = 1; i < total; i++) {
            // 在内容上方加水印
            content = stamper.getOverContent(i);
            // 在内容下方加水印
            // content = stamper.getUnderContent(i);
            content.saveState();
            content.setGState(gs);

            // 设置字体和字体大小,颜色
            content.beginText();
            content.setColorFill(BaseColor.RED);
            content.setFontAndSize(base, 24);

            // 获取每一页的高度、宽度
            pageSizeWithRotation = reader.getPageSizeWithRotation(i);
            float pageHeight = pageSizeWithRotation.getHeight();
            float pageWidth = pageSizeWithRotation.getWidth();

            // 根据纸张大小多次添加， 水印文字成30度角倾斜
            for (int height = interval + textH; height < pageHeight; height = height + textH * 11) {
                for (int width = interval + textW; width < pageWidth + textW; width = width + textW * 2) {
                    content.showTextAligned(Element.ALIGN_LEFT, waterMarkName, width - textW, height - textH, 30);
                }
            }
            content.endText();
        }

        // 关流
        stamper.close();
        reader.close();
    }

    public static void watermarkPDF(String outfile, String text) {
        PdfReader reader = null;
        PdfStamper stamper = null;
        try {
            reader = new PdfReader(toByteArray(outfile));
            PdfReader.unethicalreading = true;
            // 页数
            int n = reader.getNumberOfPages();
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
//            BaseFont baseFont = BaseFont.createFont(getChineseFont(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);

            // 设置字体大小
            Font f = new Font(baseFont, 48);
            Phrase p = new Phrase(text, f);
            stamper = new PdfStamper(reader, new FileOutputStream(outfile));
            // transparency
            PdfGState gs1 = new PdfGState();
            // 设置水印透明度
            gs1.setFillOpacity(0.3f);
            // properties
            PdfContentByte over;
            Rectangle pagesize;
            float x, y;
            for (int i = 1; i <= n; i++) {
                pagesize = reader.getPageSizeWithRotation(i);
                x = (pagesize.getLeft() + pagesize.getRight()) / 2;
                y = (pagesize.getTop() + pagesize.getBottom()) / 2;
                over = stamper.getOverContent(i);
                over.saveState();
                over.setGState(gs1);
                over.setColorFill(BaseColor.RED);
                ColumnText.showTextAligned(over, Element.ALIGN_CENTER, p, x, y, 45);
                over.restoreState();
            }
        } catch (IOException | DocumentException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (stamper != null) {
                try {
                    stamper.close();
                } catch (Exception ignored) {
                }
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static void watermarkPDF(byte[] data, String text, OutputStream outputStream) {
        PdfReader reader = null;
        PdfStamper stamper = null;
        try {
            reader = new PdfReader(data);
            PdfReader.unethicalreading = true;
            // 页数
            int n = reader.getNumberOfPages();
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
//            BaseFont baseFont = BaseFont.createFont(getChineseFont(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);

            // 设置字体大小
            Font f = new Font(baseFont, 48);
            Phrase p = new Phrase(text, f);
            stamper = new PdfStamper(reader, outputStream);
            // transparency
            PdfGState gs1 = new PdfGState();
            // 设置水印透明度
            gs1.setFillOpacity(0.3f);
            // properties
            PdfContentByte over;
            Rectangle pagesize;
            float x, y;
            for (int i = 1; i <= n; i++) {
                pagesize = reader.getPageSizeWithRotation(i);
                x = (pagesize.getLeft() + pagesize.getRight()) / 2;
                y = (pagesize.getTop() + pagesize.getBottom()) / 2;
                over = stamper.getOverContent(i);
                over.saveState();
                over.setGState(gs1);
                over.setColorFill(BaseColor.RED);
                ColumnText.showTextAligned(over, Element.ALIGN_CENTER, p, x, y, 45);
                over.restoreState();
            }
        } catch (IOException | DocumentException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (stamper != null) {
                try {
                    stamper.close();
                } catch (Exception ignored) {
                }
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static void watermarkPDF(InputStream inputStream, String text, OutputStream outputStream) {
        PdfReader reader = null;
        PdfStamper stamper = null;
        try {
            reader = new PdfReader(inputStream);
            PdfReader.unethicalreading = true;
            // 页数
            int n = reader.getNumberOfPages();
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
//            BaseFont baseFont = BaseFont.createFont(getChineseFont(), BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);

            // 设置字体大小
            Font f = new Font(baseFont, 48);
            Phrase p = new Phrase(text, f);
            stamper = new PdfStamper(reader, outputStream);
            // transparency
            PdfGState gs1 = new PdfGState();
            // 设置水印透明度
            gs1.setFillOpacity(0.3f);
            // properties
            PdfContentByte over;
            Rectangle pagesize;
            float x, y;
            for (int i = 1; i <= n; i++) {
                pagesize = reader.getPageSizeWithRotation(i);
                x = (pagesize.getLeft() + pagesize.getRight()) / 2;
                y = (pagesize.getTop() + pagesize.getBottom()) / 2;
                over = stamper.getOverContent(i);
                over.saveState();
                over.setGState(gs1);
                over.setColorFill(BaseColor.RED);
                ColumnText.showTextAligned(over, Element.ALIGN_CENTER, p, x, y, 45);
                over.restoreState();
            }
        } catch (IOException | DocumentException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (stamper != null) {
                try {
                    stamper.close();
                } catch (Exception ignored) {
                }
            }
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static byte[] toByteArray(String filename) throws IOException {
        try (FileChannel fc = new RandomAccessFile(filename, "r").getChannel()) {
            MappedByteBuffer byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()).load();
            LOGGER.info("" + byteBuffer.isLoaded());
            byte[] result = new byte[(int) fc.size()];
            if (byteBuffer.remaining() > 0) {
                byteBuffer.get(result, 0, byteBuffer.remaining());
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
//        watermarkPDF("/Users/chaoma/Downloads/123.pdf", "fevre 测试");


        // 要输出的pdf文件
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File("/Users/chaoma/Downloads/12311.pdf")));
        // 将pdf文件先加水印然后输出
        setWatermark(bos, "/Users/chaoma/Downloads/123.pdf", "SEI业务协同平台6.0");
//
//        File file = new File("/Users/chaoma/Downloads/123.pdf");
//        byte[] data = FileUtils.readFileToByteArray(file);
//        File targetFile = new File("/Users/chaoma/Downloads/12311.pdf");
////        FileOutputStream out = new FileOutputStream(targetFile);
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        watermarkPDF(data, "123SEI 测试", out);
//        out.flush();
//        FileUtils.writeByteArrayToFile(targetFile, out.toByteArray());
//        out.close();
    }
}
