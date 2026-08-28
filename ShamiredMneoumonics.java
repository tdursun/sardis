import java.math.BigInteger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ShamiredMneoumonics {

    static SecureRandom rnd = new SecureRandom();
    private static final BigInteger PRIME = BigInteger.probablePrime(4096, rnd);
    private static final URI BIP39_WORD_LIST = URI.create(
            "https://raw.githubusercontent.com/bitcoin/bips/master/bip-0039/english.txt");
    // Secret'tan daha büyük bir asal sayı
    // 24 kelimelik bir BIP39 mnemonic yaklaşık 150-250 byte olabilir.
    //PRIME =  new BigInteger("208351617316091241234326746312124448251235562226470491514186331217050270460481");

    static class Share {
        int x;
        BigInteger y;

        Share(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return x + ":" + y.toString(16);
        }
    }

    private static String createRandomMnemonic() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(BIP39_WORD_LIST).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new IOException("BIP39 kelime listesi alınamadı: HTTP " + response.statusCode());
        }

        List<String> words = response.body().lines().toList();
        if (words.size() != 2048) {
            throw new IOException("BIP39 kelime listesi eksik veya hatalı.");
        }

        StringBuilder mnemonic = new StringBuilder();

        for (int index = 0; index < 12; index++) {
            if (index > 0) {
                mnemonic.append(' ');
            }
            mnemonic.append(words.get(rnd.nextInt(words.size())));
        }

        return mnemonic.toString();
    }

    public static List<Share> split(String mnemonic) {

        SecureRandom rnd = new SecureRandom();

        BigInteger secret =
                new BigInteger(1, mnemonic.getBytes(StandardCharsets.UTF_8));

        if (secret.compareTo(PRIME) >= 0) {
            throw new IllegalArgumentException("Secret çok büyük.");
        }

        // f(x) = secret + a*x
        BigInteger a = new BigInteger(PRIME.bitLength() - 1, rnd);

        List<Share> shares = new ArrayList<>();

        for (int x = 1; x <= 3; x++) {
            BigInteger bx = BigInteger.valueOf(x);

            BigInteger y = secret
                    .add(a.multiply(bx))
                    .mod(PRIME);

            shares.add(new Share(x, y));
        }

        return shares;
    }

    public static String recover(Share s1, Share s2) {

        BigInteger x1 = BigInteger.valueOf(s1.x);
        BigInteger x2 = BigInteger.valueOf(s2.x);

        BigInteger numerator1 = x2.negate().mod(PRIME);
        BigInteger denominator1 = x1.subtract(x2).mod(PRIME);

        BigInteger numerator2 = x1.negate().mod(PRIME);
        BigInteger denominator2 = x2.subtract(x1).mod(PRIME);

        BigInteger term1 = s1.y
                .multiply(numerator1)
                .multiply(denominator1.modInverse(PRIME))
                .mod(PRIME);

        BigInteger term2 = s2.y
                .multiply(numerator2)
                .multiply(denominator2.modInverse(PRIME))
                .mod(PRIME);

        BigInteger secret = term1.add(term2).mod(PRIME);

        byte[] secretBytes = secret.toByteArray();

        if (secretBytes[0] == 0) {
            byte[] trimmed = new byte[secretBytes.length - 1];
            System.arraycopy(secretBytes, 1, trimmed, 0, trimmed.length);
            secretBytes = trimmed;
        }

        return new String(secretBytes, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        String mnemonic = createRandomMnemonic();

        List<Share> shares = split(mnemonic);

        System.out.println("Mnemonic: " + mnemonic);
        System.out.println("Shares: " + shares);

        String restored = recover(
                shares.get(0),
                shares.get(2)
        );

        System.out.println("\nRestored:");
        System.out.println(restored);
    }
}