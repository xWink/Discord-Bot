package command.util.art;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** Writes a list of frames as a GIF, with a delay per frame and an optional endless loop. */
public final class GifWriter {

    private GifWriter() { }

    /**
     * @param frames the frames, all the same size
     * @param delaysMs how long each frame shows, in milliseconds (one per frame)
     * @param loop true to repeat forever, false to play once and stop on the last frame
     * @return the GIF bytes
     */
    public static byte[] write(List<BufferedImage> frames, List<Integer> delaysMs, boolean loop) throws IOException {
        System.setProperty("java.awt.headless", "true");
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.prepareWriteSequence(null);
            for (int i = 0; i < frames.size(); i++) {
                BufferedImage img = frames.get(i);
                writer.writeToSequence(new IIOImage(img, null, metadata(writer, img, delaysMs.get(i), i == 0 && loop)),
                        null);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    /** Same delay for every frame. */
    public static byte[] write(List<BufferedImage> frames, int delayMs, boolean loop) throws IOException {
        List<Integer> delays = new java.util.ArrayList<>();
        for (int i = 0; i < frames.size(); i++) delays.add(delayMs);
        return write(frames, delays, loop);
    }

    private static IIOMetadata metadata(ImageWriter writer, BufferedImage img, int delayMs, boolean loopForever)
            throws IOException {
        IIOMetadata md = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img), null);
        String format = "javax_imageio_gif_image_1.0";
        IIOMetadataNode root = (IIOMetadataNode) md.getAsTree(format);
        IIOMetadataNode gce = child(root, "GraphicControlExtension");
        gce.setAttribute("disposalMethod", "restoreToBackgroundColor");
        gce.setAttribute("delayTime", Integer.toString(Math.max(2, delayMs / 10)));
        if (loopForever) {
            IIOMetadataNode apps = child(root, "ApplicationExtensions");
            IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
            app.setAttribute("applicationID", "NETSCAPE");
            app.setAttribute("authenticationCode", "2.0");
            app.setUserObject(new byte[]{1, 0, 0});
            apps.appendChild(app);
        }
        md.setFromTree(format, root);
        return md;
    }

    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) return (IIOMetadataNode) root.item(i);
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }
}
