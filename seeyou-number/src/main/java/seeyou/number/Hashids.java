package seeyou.number;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Septian Yudhistira
 * @version 1.0
 * @since 2025-01-06
 */
public class Hashids {
    private static Hashids hashids;
    private static final String DEFAULT_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    private final AtomicInteger stan;
    private String salt;
    private String alphabet;
    private String seps;
    private int minHashLength;
    private String guards;

    public Hashids() {
        this("");
    }

    public Hashids(String salt) {
        this(salt, 0);
    }

    public Hashids(String salt, int minHashLength) {
        this(salt, minHashLength, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
    }

    public Hashids(String salt, int minHashLength, String alphabet) {
        this.stan = new AtomicInteger(0);
        this.salt = "";
        this.alphabet = "";
        this.seps = "LH6shIHJn2XJ1O";
        this.minHashLength = 0;
        this.salt = salt;
        if (minHashLength < 0) {
            this.minHashLength = 0;
        } else {
            this.minHashLength = minHashLength;
        }

        this.alphabet = alphabet;
        String uniqueAlphabet = "";

        for(int i = 0; i < this.alphabet.length(); ++i) {
            if (!uniqueAlphabet.contains("" + this.alphabet.charAt(i))) {
                uniqueAlphabet = uniqueAlphabet + "" + this.alphabet.charAt(i);
            }
        }

        this.alphabet = uniqueAlphabet;
        int minAlphabetLength = 16;
        if (this.alphabet.length() < minAlphabetLength) {
            throw new IllegalArgumentException("alphabet must contain at least " + minAlphabetLength + " unique characters");
        } else if (this.alphabet.contains(" ")) {
            throw new IllegalArgumentException("alphabet cannot contains spaces");
        } else {
            for(int i = 0; i < this.seps.length(); ++i) {
                int j = this.alphabet.indexOf(this.seps.charAt(i));
                if (j == -1) {
                    this.seps = this.seps.substring(0, i) + " " + this.seps.substring(i + 1);
                } else {
                    this.alphabet = this.alphabet.substring(0, j) + " " + this.alphabet.substring(j + 1);
                }
            }

            this.alphabet = this.alphabet.replaceAll("\\s+", "");
            this.seps = this.seps.replaceAll("\\s+", "");
            this.seps = this.consistentShuffle(this.seps, this.salt);
            double sepDiv = 3.5D;
            int diff;
            if (this.seps.equals("") || (double)(this.alphabet.length() / this.seps.length()) > sepDiv) {
                int seps_len = (int)Math.ceil((double)this.alphabet.length() / sepDiv);
                if (seps_len == 1) {
                    ++seps_len;
                }

                if (seps_len > this.seps.length()) {
                    diff = seps_len - this.seps.length();
                    this.seps = this.seps + this.alphabet.substring(0, diff);
                    this.alphabet = this.alphabet.substring(diff);
                } else {
                    this.seps = this.seps.substring(0, seps_len);
                }
            }

            this.alphabet = this.consistentShuffle(this.alphabet, this.salt);
            int guardDiv = 12;
            diff = (int)Math.ceil((double)this.alphabet.length() / (double)guardDiv);
            if (this.alphabet.length() < 3) {
                this.guards = this.seps.substring(0, diff);
                this.seps = this.seps.substring(diff);
            } else {
                this.guards = this.alphabet.substring(0, diff);
                this.alphabet = this.alphabet.substring(diff);
            }

        }
    }

    public static Hashids getInstance() {
        Class var0 = Hashids.class;
        synchronized(Hashids.class) {
            if (hashids == null) {
                hashids = new Hashids();
            }
        }

        return hashids;
    }

    /** @deprecated */
    @Deprecated
    public String encrypt(long... numbers) {
        return this.encode(numbers);
    }

    /** @deprecated */
    @Deprecated
    public long[] decrypt(String hash) {
        return this.decode(hash);
    }

    /** @deprecated */
    @Deprecated
    public String encryptHex(String hexa) {
        return this.encodeHex(hexa);
    }

    /** @deprecated */
    @Deprecated
    public String decryptHex(String hash) {
        return this.decodeHex(hash);
    }

    public String encode(long... numbers) {
        long[] var2 = numbers;
        int var3 = numbers.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            long number = var2[var4];
            if (number > 9007199254740992L) {
                throw new IllegalArgumentException("number can not be greater than 9007199254740992L");
            }
        }

        String retval = "";
        if (numbers.length == 0) {
            return retval;
        } else {
            return this._encode(numbers);
        }
    }

    public long[] decode(String hash) {
        long[] ret = new long[0];
        return hash.equals("") ? ret : this._decode(hash, this.alphabet);
    }

    public String encodeHex(String hexa) {
        if (!hexa.matches("^[0-9a-fA-F]+$")) {
            return "";
        } else {
            List<Long> matched = new ArrayList();
            Matcher matcher = Pattern.compile("[\\w\\W]{1,12}").matcher(hexa);

            while(matcher.find()) {
                matched.add(Long.parseLong("1" + matcher.group(), 16));
            }

            long[] result = new long[matched.size()];

            for(int i = 0; i < matched.size(); ++i) {
                result[i] = (Long)matched.get(i);
            }

            return this._encode(result);
        }
    }

    public String decodeHex(String hash) {
        String result = "";
        long[] numbers = this.decode(hash);
        long[] var4 = numbers;
        int var5 = numbers.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            long number = var4[var6];
            result = result + Long.toHexString(number).substring(1);
        }

        return result;
    }

    private String _encode(long... numbers) {
        int numberHashInt = 0;

        for(int i = 0; i < numbers.length; ++i) {
            numberHashInt = (int)((long)numberHashInt + numbers[i] % (long)(i + 100));
        }

        String alphabet = this.alphabet;
        char ret = alphabet.toCharArray()[numberHashInt % alphabet.length()];
        String ret_str = ret + "";

        int halfLen;
        for(halfLen = 0; halfLen < numbers.length; ++halfLen) {
            long num = numbers[halfLen];
            String buffer = ret + this.salt + alphabet;
            alphabet = this.consistentShuffle(alphabet, buffer.substring(0, alphabet.length()));
            String last = this.hash(num, alphabet);
            ret_str = ret_str + last;
            if (halfLen + 1 < numbers.length) {
                num %= (long)(last.toCharArray()[0] + halfLen);
                int sepsIndex = (int)(num % (long)this.seps.length());
                ret_str = ret_str + this.seps.toCharArray()[sepsIndex];
            }
        }

        if (ret_str.length() < this.minHashLength) {
            int guardIndex = (numberHashInt + ret_str.toCharArray()[0]) % this.guards.length();
            char guard = this.guards.toCharArray()[guardIndex];
            ret_str = guard + ret_str;
            if (ret_str.length() < this.minHashLength) {
                guardIndex = (numberHashInt + ret_str.toCharArray()[2]) % this.guards.length();
                guard = this.guards.toCharArray()[guardIndex];
                ret_str = ret_str + guard;
            }
        }

        halfLen = alphabet.length() / 2;

        while(ret_str.length() < this.minHashLength) {
            alphabet = this.consistentShuffle(alphabet, alphabet);
            ret_str = alphabet.substring(halfLen) + ret_str + alphabet.substring(0, halfLen);
            int excess = ret_str.length() - this.minHashLength;
            if (excess > 0) {
                int start_pos = excess / 2;
                ret_str = ret_str.substring(start_pos, start_pos + this.minHashLength);
            }
        }

        return ret_str;
    }

    private long[] _decode(String hash, String alphabet) {
        ArrayList<Long> ret = new ArrayList();
        int i = 0;
        String regexp = "[" + this.guards + "]";
        String hashBreakdown = hash.replaceAll(regexp, " ");
        String[] hashArray = hashBreakdown.split(" ");
        if (hashArray.length == 3 || hashArray.length == 2) {
            i = 1;
        }

        hashBreakdown = hashArray[i];
        char lottery = hashBreakdown.toCharArray()[0];
        hashBreakdown = hashBreakdown.substring(1);
        hashBreakdown = hashBreakdown.replaceAll("[" + this.seps + "]", " ");
        hashArray = hashBreakdown.split(" ");
        String[] var11 = hashArray;
        int k = hashArray.length;

        for(int var13 = 0; var13 < k; ++var13) {
            String aHashArray = var11[var13];
            String buffer = lottery + this.salt + alphabet;
            alphabet = this.consistentShuffle(alphabet, buffer.substring(0, alphabet.length()));
            ret.add(this.unhash(aHashArray, alphabet));
        }

        long[] arr = new long[ret.size()];

        for(k = 0; k < arr.length; ++k) {
            arr[k] = (Long)ret.get(k);
        }

        if (!this._encode(arr).equals(hash)) {
            arr = new long[0];
        }

        return arr;
    }

    private String consistentShuffle(String alphabet, String salt) {
        if (salt.length() <= 0) {
            return alphabet;
        } else {
            char[] arr = salt.toCharArray();
            int i = alphabet.length() - 1;
            int v = 0;

            for(int p = 0; i > 0; ++v) {
                v %= salt.length();
                int asc_val = arr[v];
                p += asc_val;
                int j = (asc_val + v + p) % i;
                char tmp = alphabet.charAt(j);
                alphabet = alphabet.substring(0, j) + alphabet.charAt(i) + alphabet.substring(j + 1);
                alphabet = alphabet.substring(0, i) + tmp + alphabet.substring(i + 1);
                --i;
            }

            return alphabet;
        }
    }

    private String hash(long input, String alphabet) {
        String hash = "";
        int alphabetLen = alphabet.length();
        char[] arr = alphabet.toCharArray();

        do {
            hash = arr[(int)(input % (long)alphabetLen)] + hash;
            input /= (long)alphabetLen;
        } while(input > 0L);

        return hash;
    }

    private Long unhash(String input, String alphabet) {
        long number = 0L;
        char[] input_arr = input.toCharArray();

        for(int i = 0; i < input.length(); ++i) {
            long pos = (long)alphabet.indexOf(input_arr[i]);
            number = (long)((double)number + (double)pos * Math.pow((double)alphabet.length(), (double)(input.length() - i - 1)));
        }

        return number;
    }

    public static int checkedCast(long value) {
        int result = (int)value;
        if ((long)result != value) {
            throw new IllegalArgumentException("Out of range: " + value);
        } else {
            return result;
        }
    }

    public String getVersion() {
        return "1.0.0";
    }

    public Long getCounter() {
        if (this.stan.get() >= 1000000) {
            this.stan.set(0);
        }

        long incrementAndGet = (long)this.stan.incrementAndGet();

        String stanString;
        for(stanString = String.valueOf(incrementAndGet); stanString.length() < 6; stanString = "0" + stanString) {
        }

        String newStan = (new SimpleDateFormat("yyyyMMdd")).format(new Date()) + stanString;
        return Long.valueOf(newStan);
    }

    public String getEncodeId() {
        return this.encode(this.getCounter());
    }

    public void setSalt(String mysalt) {
        this.salt = mysalt;
    }

    public void setStan(int i) {
        this.stan.set(i);
    }

    public int getStan() {
        return this.stan.get();
    }

}
