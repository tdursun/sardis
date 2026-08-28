import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public class ShareQrCodeGenerator {

    private static final int QR_SIZE = 1000;

    public static void main(String[] args) throws IOException, WriterException {
        if (args.length == 0 || args.length > 3) {
            System.err.println("Kullanim: java ShareQrCodeGenerator <share1> [share2] [share3]");
            System.exit(1);
        }

        for (String share : args) {
            int separator = share.indexOf(':');
            if (separator <= 0) {
                throw new IllegalArgumentException("Share formati gecersiz: " + share);
            }

            String shareId = share.substring(0, separator);
            if (!shareId.matches("[1-9][0-9]*")) {
                throw new IllegalArgumentException("Share x degeri gecersiz: " + share);
            }

            Path output = Path.of("share-" + shareId + ".png");
            writeQrCode(share, output);
            System.out.println("Olusturuldu: " + output.toAbsolutePath());
        }
    }

    private static void writeQrCode(String share, Path output)
            throws IOException, WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);

        BitMatrix matrix = new MultiFormatWriter().encode(
                share,
                BarcodeFormat.QR_CODE,
                QR_SIZE,
                QR_SIZE,
                hints);

        MatrixToImageWriter.writeToPath(matrix, "PNG", output);
    }
}
