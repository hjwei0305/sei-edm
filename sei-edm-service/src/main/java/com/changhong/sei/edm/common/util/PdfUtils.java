package com.changhong.sei.edm.common.util;

import com.changhong.sei.util.FileUtils;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static PDExtendedGraphicsState r0;

    static {
        r0 = new PDExtendedGraphicsState();
        // 透明度
        r0.setNonStrokingAlphaConstant(0.2f);
        r0.setAlphaSourceFlag(true);
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

    public static void main(String[] args) throws IOException {
//        watermarkPDF("/Users/chaoma/Downloads/123.pdf", "fevre 测试");

        File file = new File("/Users/chaoma/Downloads/123.pdf");
        byte[] data = FileUtils.readFileToByteArray(file);
        File targetFile = new File("/Users/chaoma/Downloads/12311.pdf");
//        FileOutputStream out = new FileOutputStream(targetFile);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        watermarkPDF(data, "123SEI 测试", out);
        out.flush();
        FileUtils.writeByteArrayToFile(targetFile, out.toByteArray());
        out.close();
    }
}
