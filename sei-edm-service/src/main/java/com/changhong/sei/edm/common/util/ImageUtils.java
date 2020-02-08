package com.changhong.sei.edm.common.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.*;
import java.util.Objects;

/**
 * 实现功能：图片处理工具类
 * 功能：缩放图像、切割图像、图像类型转换、彩色转黑白、文字水印、图片水印等
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2019-10-11 14:33
 */
public class ImageUtils {
    /**
     * 图形交换格式
     */
    public static String IMAGE_TYPE_GIF = "GIF";
    /**
     * 联合照片专家组
     */
    public static String IMAGE_TYPE_JPG = "JPG";
    /**
     * 联合照片专家组
     */
    public static String IMAGE_TYPE_JPEG = "JPEG";
    /**
     * 英文Bitmap（位图）的简写，它是Windows操作系统中的标准图像文件格式
     */
    public static String IMAGE_TYPE_BMP = "BMP";
    /**
     * 可移植网络图形
     */
    public static String IMAGE_TYPE_PNG = "PNG";
    /**
     * Photoshop的专用格式Photoshop
     */
    public static String IMAGE_TYPE_PSD = "PSD";

    /**
     * 缩放图像（按比例缩放）
     *
     * @param srcImageFile 源图像文件地址
     * @param result       缩放后的图像地址
     * @param scale        缩放比例
     * @param flag         缩放选择:true 放大; false 缩小;
     */
    public static void scale(String srcImageFile, String result, int scale, boolean flag) {
        try {
            // 读入文件
            BufferedImage src = ImageIO.read(new File(srcImageFile));
            BufferedImage tag = scaleImage(src, scale, flag);
            // 输出到文件流
            ImageIO.write(tag, IMAGE_TYPE_JPEG, new File(result));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 缩放图像（按比例缩放）
     *
     * @param srcImage 源图像文件
     * @param scale    缩放比例
     * @param flag     缩放选择:true 放大; false 缩小
     */
    public static BufferedImage scaleImage(BufferedImage srcImage, int scale, boolean flag) {
        // 读入文件
        // 得到源图宽
        int width = srcImage.getWidth();
        // 得到源图长
        int height = srcImage.getHeight();

        if (flag) {
            // 放大
            width = width * scale;
            height = height * scale;
        } else {
            // 缩小
            width = width / scale;
            height = height / scale;
        }
        Image image = srcImage.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        BufferedImage tag = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = tag.getGraphics();
        // 绘制缩小后的图
        g.drawImage(image, 0, 0, null);
        g.dispose();
        // 输出到文件流
        return tag;
    }

    /**
     * 缩放图像（按高度和宽度缩放）
     *
     * @param srcImageFile 源图像文件地址
     * @param result       缩放后的图像地址
     * @param height       缩放后的高度
     * @param width        缩放后的宽度
     * @param bb           比例不对时是否需要补白：true为补白; false为不补白;
     */
    public static void scale2(String srcImageFile, String result, int height, int width, boolean bb) {
        if (Objects.isNull(srcImageFile) || srcImageFile.length() == 0) {
            return;
        }
        if (Objects.isNull(result) || result.length() == 0) {
            return;
        }
        try {
            File f = new File(srcImageFile);
            BufferedImage bi = ImageIO.read(f);

            BufferedImage buImg = scaleImage(bi, height, width, bb);
            ImageIO.write(buImg, IMAGE_TYPE_JPEG, new File(result));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 缩放图像（按高度和宽度缩放）
     *
     * @param stream 源图像文件流
     * @param height 缩放后的高度
     * @param width  缩放后的宽度
     * @param bb     比例不对时是否需要补白：true为补白; false为不补白;
     */
    public static byte[] scale2(InputStream stream, String extension, int height, int width, boolean bb) {
        if (Objects.isNull(stream)) {
            return null;
        }
        if (Objects.isNull(extension) || extension.length() == 0) {
            extension = IMAGE_TYPE_PNG;
        }
        try {
            BufferedImage bi = ImageIO.read(stream);
            BufferedImage buImg = scaleImage(bi, height, width, bb);

            // 将图片保存为数据流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(buImg, extension, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * 缩放图像（按高度和宽度缩放）
     *
     * @param srcImage 源图像文件
     * @param height   缩放后的高度
     * @param width    缩放后的宽度
     * @param bb       比例不对时是否需要补白：true为补白; false为不补白;
     */
    public static BufferedImage scaleImage(BufferedImage srcImage, int height, int width, boolean bb) {
        // 缩放比例
        double ratio;
        int imgWidth = srcImage.getWidth();
        int imgHeight = srcImage.getHeight();

        Image itemp = srcImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        // 计算比例
        if ((imgHeight > height) || (imgWidth > width)) {
            if (imgHeight > srcImage.getWidth()) {
                ratio = (new Integer(height)).doubleValue() / imgHeight;
            } else {
                ratio = (new Integer(width)).doubleValue() / imgWidth;
            }
            AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(ratio, ratio), null);
            itemp = op.filter(srcImage, null);
        }
        if (bb) {
            // 补白
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.white);
            g.fillRect(0, 0, width, height);
            if (width == itemp.getWidth(null)) {
                g.drawImage(itemp, 0, (height - itemp.getHeight(null)) / 2,
                        itemp.getWidth(null), itemp.getHeight(null),
                        Color.white, null);
            } else {
                g.drawImage(itemp, (width - itemp.getWidth(null)) / 2, 0,
                        itemp.getWidth(null), itemp.getHeight(null),
                        Color.white, null);
            }
            g.dispose();
            itemp = image;
        }
        return (BufferedImage) itemp;
    }

    /**
     * 图像切割(按指定起点坐标和宽高切割)
     *
     * @param srcImageFile 源图像地址
     * @param result       切片后的图像地址
     * @param x            目标切片起点坐标X
     * @param y            目标切片起点坐标Y
     * @param width        目标切片宽度
     * @param height       目标切片高度
     */
    public static void cut(String srcImageFile, String result, int x, int y, int width, int height) {
        try {
            // 读取源图像
            BufferedImage bi = ImageIO.read(new File(srcImageFile));
            // 源图宽度
            int srcWidth = bi.getHeight();
            // 源图高度
            int srcHeight = bi.getWidth();
            if (srcWidth > 0 && srcHeight > 0) {
                Image image = bi.getScaledInstance(srcWidth, srcHeight, Image.SCALE_DEFAULT);
                // 四个参数分别为图像起点坐标和宽高 即: CropImageFilter(int x,int y,int width,int height)
                ImageFilter cropFilter = new CropImageFilter(x, y, width, height);
                Image img = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(), cropFilter));
                BufferedImage tag = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics g = tag.getGraphics();
                // 绘制切割后的图
                g.drawImage(img, 0, 0, width, height, null);
                g.dispose();
                // 输出为文件
                ImageIO.write(tag, IMAGE_TYPE_JPEG, new File(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 函 数 名: cut 剪切图标
     *
     * @param image  内存中的图片 BufferedImage对象
     * @param x      开始剪切的x坐标
     * @param y      开始剪切的y坐标
     * @param width  剪切的宽度
     * @param height 剪切的高度
     * @return 剪切后的image
     */
    public static BufferedImage cut(BufferedImage image, int x, int y, int width, int height) {
        return image.getSubimage(x, y, width, height);
    }

    /**
     * 图像切割（指定切片的行数和列数）
     *
     * @param srcImageFile 源图像地址
     * @param descDir      切片目标文件夹
     * @param rows         目标切片行数。默认2，必须是范围 [1, 20] 之内
     * @param cols         目标切片列数。默认2，必须是范围 [1, 20] 之内
     */
    public static void cut2(String srcImageFile, String descDir, int rows, int cols) {
        try {
            if (rows <= 0 || rows > 20) {
                // 切片行数
                rows = 2;
            }
            if (cols <= 0 || cols > 20) {
                // 切片列数
                cols = 2;
            }
            // 读取源图像
            BufferedImage bi = ImageIO.read(new File(srcImageFile));
            // 源图宽度
            int srcWidth = bi.getHeight();
            // 源图高度
            int srcHeight = bi.getWidth();
            if (srcWidth > 0 && srcHeight > 0) {
                Image img;
                ImageFilter cropFilter;
                Image image = bi.getScaledInstance(srcWidth, srcHeight, Image.SCALE_DEFAULT);
                // 每张切片的宽度
                int destWidth = srcWidth;
                // 每张切片的高度
                int destHeight = srcHeight;
                // 计算切片的宽度和高度
                if (srcWidth % cols == 0) {
                    destWidth = srcWidth / cols;
                } else {
                    destWidth = (int) Math.floor(srcWidth / cols) + 1;
                }
                if (srcHeight % rows == 0) {
                    destHeight = srcHeight / rows;
                } else {
                    destHeight = (int) Math.floor(srcWidth / rows) + 1;
                }
                // 循环建立切片 改进的想法:是否可用多线程加快切割速度
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        // 四个参数分别为图像起点坐标和宽高 即: CropImageFilter(int x,int y,int width,int height)
                        cropFilter = new CropImageFilter(j * destWidth, i * destHeight, destWidth, destHeight);
                        img = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(image.getSource(), cropFilter));
                        BufferedImage tag = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics g = tag.getGraphics();
                        // 绘制缩小后的图
                        g.drawImage(img, 0, 0, null);
                        g.dispose();
                        // 输出为文件
                        ImageIO.write(tag, IMAGE_TYPE_JPEG, new File(descDir + "_r" + i + "_c" + j + ".jpg"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 图像切割（指定切片的宽度和高度）
     *
     * @param srcImageFile 源图像地址
     * @param descDir      切片目标文件夹
     * @param destWidth    目标切片宽度。默认200
     * @param destHeight   目标切片高度。默认150
     */
    public static void cut3(String srcImageFile, String descDir, int destWidth, int destHeight) {
        try {
            if (destWidth <= 0) {
                // 切片宽度
                destWidth = 200;
            }
            if (destHeight <= 0) {
                // 切片高度
                destHeight = 150;
            }
            // 读取源图像
            BufferedImage bi = ImageIO.read(new File(srcImageFile));
            // 源图宽度
            int srcWidth = bi.getHeight();
            // 源图高度
            int srcHeight = bi.getWidth();
            if (srcWidth > destWidth && srcHeight > destHeight) {
                Image img;
                ImageFilter cropFilter;
                Image image = bi.getScaledInstance(srcWidth, srcHeight, Image.SCALE_DEFAULT);
                // 切片横向数量
                int cols = 0;
                // 切片纵向数量
                int rows = 0;
                // 计算切片的横向和纵向数量
                if (srcWidth % destWidth == 0) {
                    cols = srcWidth / destWidth;
                } else {
                    cols = (int) Math.floor(srcWidth / destWidth) + 1;
                }
                if (srcHeight % destHeight == 0) {
                    rows = srcHeight / destHeight;
                } else {
                    rows = (int) Math.floor(srcHeight / destHeight) + 1;
                }
                // 循环建立切片 改进的想法:是否可用多线程加快切割速度
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        // 四个参数分别为图像起点坐标和宽高 即: CropImageFilter(int x,int y,int width,int height)
                        cropFilter = new CropImageFilter(j * destWidth, i * destHeight, destWidth, destHeight);
                        img = Toolkit.getDefaultToolkit().createImage(
                                new FilteredImageSource(image.getSource(), cropFilter));
                        BufferedImage tag = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics g = tag.getGraphics();
                        // 绘制缩小后的图
                        g.drawImage(img, 0, 0, null);
                        g.dispose();
                        // 输出为文件
                        ImageIO.write(tag, IMAGE_TYPE_JPEG, new File(descDir + "_r" + i + "_c" + j + ".jpg"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 图像类型转换：GIF->JPG、GIF->PNG、PNG->JPG、PNG->GIF(X)、BMP->PNG
     *
     * @param srcImageFile  源图像地址
     * @param formatName    包含格式非正式名称的 String：如JPG、JPEG、GIF等
     * @param destImageFile 目标图像地址
     */
    public static void convert(String srcImageFile, String formatName, String destImageFile) {
        try {
            File f = new File(srcImageFile);
            f.canRead();
            f.canWrite();
            BufferedImage src = ImageIO.read(f);
            ImageIO.write(src, formatName, new File(destImageFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 彩色转为黑白
     *
     * @param srcImageFile  源图像地址
     * @param destImageFile 目标图像地址
     */
    public static void gray(String srcImageFile, String destImageFile) {
        try {
            BufferedImage src = ImageIO.read(new File(srcImageFile));
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            ColorConvertOp op = new ColorConvertOp(cs, null);
            src = op.filter(src, null);
            ImageIO.write(src, IMAGE_TYPE_JPEG, new File(destImageFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 给图片添加文字水印
     *
     * @param pressText     水印文字
     * @param srcImageFile  源图像地址
     * @param destImageFile 目标图像地址
     * @param fontName      水印的字体名称
     * @param fontStyle     水印的字体样式
     * @param color         水印的字体颜色
     * @param fontSize      水印的字体大小
     * @param x             修正值
     * @param y             修正值
     * @param alpha         透明度：alpha 必须是范围 [0.0, 1.0] 之内（包含边界值）的一个浮点数字
     */
    public static void pressText(String pressText, String srcImageFile,
                                 String destImageFile, String fontName, int fontStyle, Color color,
                                 int fontSize, int x, int y, float alpha) {
        try {
            File img = new File(srcImageFile);
            Image src = ImageIO.read(img);
            int width = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, width, height, null);
            g.setColor(color);
            g.setFont(new Font(fontName, fontStyle, fontSize));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
            // 在指定坐标绘制水印文字
            g.drawString(pressText, (width - (getLength(pressText) * fontSize)) / 2 + x, (height - fontSize) / 2 + y);
            g.dispose();
            // 输出到文件流
            ImageIO.write(image, IMAGE_TYPE_JPEG, new File(destImageFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void pressText(String pressText, InputStream in, OutputStream os, String fontName, int fontStyle, Color color,
                                 int fontSize, int x, int y, float alpha) {
        try {
            Image src = ImageIO.read(in);
            int width = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, width, height, null);
            g.setColor(color);
            g.setFont(new Font(fontName, fontStyle, fontSize));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
            // 在指定坐标绘制水印文字
            g.drawString(pressText, (width - (getLength(pressText) * fontSize)) / 2 + x, (height - fontSize) / 2 + y);
            g.dispose();
            // 输出到文件流
            ImageIO.write(image, IMAGE_TYPE_JPEG, os);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 给图片添加图片水印
     *
     * @param pressImg      水印图片
     * @param srcImageFile  源图像地址
     * @param destImageFile 目标图像地址
     * @param x             修正值。 默认在中间
     * @param y             修正值。 默认在中间
     * @param alpha         透明度：alpha 必须是范围 [0.0, 1.0] 之内（包含边界值）的一个浮点数字
     */
    public static void pressImage(String pressImg, String srcImageFile,
                                  String destImageFile, int x, int y, float alpha) {
        try {
            File img = new File(srcImageFile);
            Image src = ImageIO.read(img);
            int wideth = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(wideth, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, wideth, height, null);
            // 水印文件
            Image srcBiao = ImageIO.read(new File(pressImg));
            int widethBiao = srcBiao.getWidth(null);
            int heightBiao = srcBiao.getHeight(null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
            g.drawImage(srcBiao, (wideth - widethBiao) / 2, (height - heightBiao) / 2, widethBiao, heightBiao, null);
            // 水印文件结束
            g.dispose();
            ImageIO.write(image, IMAGE_TYPE_JPEG, new File(destImageFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 计算text的长度（一个中文算两个字符）
     */
    public static int getLength(String text) {
        int length = 0;
        for (int i = 0; i < text.length(); i++) {
            if ((text.charAt(i) + "").getBytes().length > 1) {
                length += 2;
            } else {
                length += 1;
            }
        }
        return length / 2;
    }

    /**
     * 对图片进行旋转
     *
     * @param src   被旋转图片
     * @param angel 旋转角度
     * @return 旋转后的图片
     */
    public static BufferedImage rotate(Image src, int angel) {
        int srcWidth = src.getWidth(null);
        int srcHeight = src.getHeight(null);
        // 计算旋转后图片的尺寸
        Rectangle rectDes = calcRotatedSize(new Rectangle(new Dimension(srcWidth, srcHeight)), angel);
        BufferedImage res = new BufferedImage(rectDes.width, rectDes.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = res.createGraphics();
        // 进行转换
        g2.translate((rectDes.width - srcWidth) / 2, (rectDes.height - srcHeight) / 2);
        // srcWidth / 2   ---->   srcWidth >> 1
        g2.rotate(Math.toRadians(angel), srcWidth >> 1, srcHeight >> 1);

        g2.drawImage(src, null, null);
        return res;
    }

    /**
     * 计算旋转后的图片
     *
     * @param src   被旋转的图片
     * @param angel 旋转角度
     * @return 旋转后的图片
     */
    private static Rectangle calcRotatedSize(Rectangle src, int angel) {
        // 如果旋转的角度大于90度做相应的转换
        if (angel >= 90) {
            if (angel / 90 % 2 == 1) {
                int temp = src.height;
                src.height = src.width;
                src.width = temp;
            }
            angel = angel % 90;
        }

        double r = Math.sqrt(src.height * src.height + src.width * src.width) / 2;
        double len = 2 * Math.sin(Math.toRadians(angel) / 2) * r;
        double angelAlpha = (Math.PI - Math.toRadians(angel)) / 2;
        double angelDaltaWidth = Math.atan((double) src.height / src.width);
        double angelDaltaHeight = Math.atan((double) src.width / src.height);

        int lenDaltaWidth = (int) (len * Math.cos(Math.PI - angelAlpha - angelDaltaWidth));
        int lenDaltaHeight = (int) (len * Math.cos(Math.PI - angelAlpha - angelDaltaHeight));
        int desWidth = src.width + lenDaltaWidth * 2;
        int desHeight = src.height + lenDaltaHeight * 2;
        return new Rectangle(new Dimension(desWidth, desHeight));
    }

}
