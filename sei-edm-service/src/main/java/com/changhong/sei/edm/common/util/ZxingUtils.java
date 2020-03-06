package com.changhong.sei.edm.common.util;

import com.changhong.sei.core.log.LogUtil;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Vector;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2019-05-28 14:39
 */
public class ZxingUtils {

    private static final long MAX_PIXELS = 1 << 25;
    public static final Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);

    static {
//        //优化精度
//        HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
//        HINTS.put(DecodeHintType.CHARACTER_SET, "UTF-8");
//        Vector<BarcodeFormat> decodeFormats = new Vector<>();
//        decodeFormats.add(BarcodeFormat.CODE_128);
//        decodeFormats.add(BarcodeFormat.CODE_39);
//        decodeFormats.add(BarcodeFormat.CODE_93);
//        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
    }

    public static String processImage(BufferedImage image, Map<DecodeHintType, Object> hints, String[] matchPrefix) {
        if (image == null) {
            LogUtil.error("the decode image may be not exit.");
            return null;
        }
        int height = image.getHeight();
        int width = image.getWidth();
        if (height <= 1 || width <= 1 || height * width > MAX_PIXELS) {
            return null;
        }

        String result = StringUtils.EMPTY;
        Reader reader = new MultiFormatReader();
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
        ReaderException savedException = null;
        try {
            // Look for multiple barcodes
            try {
                MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(reader);
                Result[] theResults = multiReader.decodeMultiple(bitmap, hints);
                if (theResults != null) {
                    for (Result result1 : theResults) {
                        if (StringUtils.startsWithAny(result1.getText().toLowerCase(), matchPrefix)) {
                            result = result1.getText();
                        }
                    }
                    if (StringUtils.isBlank(result)) {
                        result = theResults[0].getText();
                    }
                    return result;
                }
            } catch (ReaderException re) {
                savedException = re;
            } finally {
                reader.reset();
            }

            // Look for normal barcode in photo
            try {
                Result theResult = reader.decode(bitmap, hints);
                if (theResult != null) {
                    return theResult.getText();
                }
            } catch (ReaderException re) {
                savedException = re;
            } finally {
                reader.reset();
            }

            // Try again with other binarizer
            try {
                BinaryBitmap hybridBitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result theResult = reader.decode(hybridBitmap, hints);
                if (theResult != null) {
                    return theResult.getText();
                }
            } catch (ReaderException re) {
                savedException = re;
            } finally {
                reader.reset();
            }

            // Look for pure barcode
            try {
                Map<DecodeHintType, Object> hintsPure = new EnumMap<>(hints);
                //复杂模式，开启PURE_BARCODE模式
                hintsPure.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
                Result theResult = reader.decode(bitmap, hintsPure);
                if (theResult != null) {
                    return theResult.getText();
                }
            } catch (ReaderException re) {
                savedException = re;
            } finally {
                reader.reset();
            }

            throw savedException == null ? NotFoundException.getNotFoundInstance() : savedException;
        } catch (Exception e) {
            LogUtil.error("条码识别异常", e);
        }
        return result;
    }

    public static void main(String[] args) {
//        TESS_DATA_PATH = "D:/Program Files/Tesseract-OCR/tessdata";
//        BARCODE_MATCH = StringUtils.stripAll(StringUtils.split("BX-,CCBX-, YLHDC, GWM".toLowerCase(), ","));
        File file1 = new File("D:\\data\\img20190926_16070729.jpg");
//        File file1 = new File("/Users/chaoma/Downloads/Image_00001.pdf");
//        File file1 = new File("/Users/chaoma/Downloads/123.png");
//        File file1 = new File("/Users/chaoma/Downloads/122.jpg");
//        File file2 = new File("/Users/chaoma/Downloads/300.jpg");
//        long s = System.currentTimeMillis();
//        try {
//            InputStream input = new FileInputStream(file1);
//            String barcode = ZxingUtils.decode(input, DocumentType.Image);
//            System.out.println("条码：" + barcode + "  耗时： " + (System.currentTimeMillis() - s));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }


        System.out.println(60 % 3);
        try {
            System.out.println(ZxingUtils.class.getClassLoader().getResource(""));
            URL url = ZxingUtils.class.getClassLoader().getResource("msyh.ttc");
            assert url != null;
            File file = new File(url.toURI());
            FileInputStream input = new FileInputStream(file);
            System.out.println(input);

            ClassPathResource resource = new ClassPathResource("font" + File.separator + "msyh.ttc");
            InputStream inputStream = ZxingUtils.class.getClassLoader().getResourceAsStream("font/msyh.ttc");//此方法返回读取文件字节的方式在linux系统中无异。
            System.out.println(inputStream);
            FileInputStream fis = new FileInputStream(ClassLoader.getSystemResource("").getPath() + "font/msyh.ttc");
            System.out.println(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        try {
//            InputStream input = new FileInputStream(file1);
//            InvoiceItem invoiceItem = ZxingUtils.decodeInvoic(input, DocumentType.Image);
//            System.out.println("条码：" + invoiceItem + "  耗时： " + (System.currentTimeMillis() - s));
//            String json = JsonUtils.toJson(invoiceItem);
//            System.out.println(json);
//            invoiceItem = JsonUtils.fromJson(json, InvoiceItem.class);
//            System.out.println(invoiceItem);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

//        s = System.currentTimeMillis();
//        try {
//            InputStream input = new FileInputStream(file1);
//            Set<String> barcode = ZxingUtils.decode(input, DocumentType.Pdf);
//            System.out.println("条码：" + barcode + "  耗时： " + (System.currentTimeMillis() - s));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }

}
