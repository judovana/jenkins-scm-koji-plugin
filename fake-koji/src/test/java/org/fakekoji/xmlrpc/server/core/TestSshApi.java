/*
 * The MIT License
 *
 * Copyright 2017 jvanek.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fakekoji.xmlrpc.server.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.fakekoji.xmlrpc.server.SshApiService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Warning - reaming check have missing check on content!
 *
 * @author jvanek
 */
public class TestSshApi {

    private static final String IDS_RSA = "-----BEGIN RSA PRIVATE KEY-----\n"
            + "MIISKQIBAAKCBAEA6DOkXJ7K89Ai3E+XTvFjefNmXRrYVIxx8TFonXGfUvSdNISD\n"
            + "uZW+tC9FbFNBJWZUFludQdHAczLCKLOoUq7mTBe/wPOreSyIDI1iNnawV/KsX7Ok\n"
            + "yThsDolKxgRA+we8JuUYAes2y94FKaw4kAY/Ob16WSf7AP9Y8Oa4/PcK6KCIkzQx\n"
            + "iqL+SGG3mLy+XhTU/pJYnEC8c1lw+Gah8oPWG1vx5W578iWixgTbNp0TTXNr1+jU\n"
            + "xVRg1SitC4WP8g67af6f5rhcJZt5Dz/gWajHqKkK97nmPSDttso56ueeUW3L8lM3\n"
            + "scFjGQu3QbpLmFpMZeTpePOn7CjVjfBZnocNzDdqkgE+ivEB7nWWNbgEwALX4NR4\n"
            + "DzkGGoFPUKdsIdEBC5D73XC6NJxHKWOO9L+KyUxoeA8hUHBuWc3pVSi7NyG04Pvq\n"
            + "EfO0Ea1p4Tn2nb2CreEgOyCQ/nLJJrPEDef+8GUKKs3tVawbOn6tLFyi7aFY9u35\n"
            + "KX3fgt5t8TiIqmcIxs7oC16ny/97pe7gBRZuLU4AxK76m6Nxr1lGw7wJOXllhoKb\n"
            + "Zys676qlIG6VvSw/dAUekY/Vk9duIm9BHHLVZetv/LL4hWNy7xMvlpl8V141azJw\n"
            + "BE10V1GuvJnRtsf57ZqSOjiezTewavteJLwTZlRTV7QwsVpUGM7YyYJacv1GkFzz\n"
            + "00UWg+/Pf1gyLHf+JgsWx1ftUU9WlnVgBSfZpWKHexjcKrJzk4n96HWFL4ihCfxd\n"
            + "895ZkBsbTyuRAsC3Tw+k/FC6j0bbM5eE9weBH9b3Ap6tZFlIwEwXCvU28i0SbxEq\n"
            + "PXpnxpK33XRHz3byu8QtX4BulVrwa0P/ZeY0fvooUJo+tUqHbE7Pf5DlzylZarXQ\n"
            + "BSHjPcG+bs37LA2ar/1o6hRXJ5Z3b8f4YH3HumGuiYY3NjDvq++P5RTnasWFN7+f\n"
            + "1xYexB3HZE9RXxCTTVIn09fVHnaloNCnvhl6fn40TfsmS10qN1hwh55nzvpUjqGz\n"
            + "qyC+9Z/F+RpqIIAVKyijyMy2PiJRBy8sPZa1qf9+xDCZsRYdUPebr8t38sPEYYtR\n"
            + "8wMfY1iq0qXn1Pi/G+ghmTn+/UIdubn/m7Iu/F0GSyN6cjz4fskpS5bAb1oJhoWD\n"
            + "yrx6EAf96OoVIrMQcaBkLVcd7MW59rSDK0gNK7DE3ZXa85VwcMj/Y9HCybY/Xlf5\n"
            + "v7Csi38E+ybHP2R3YEPMduLg3hrs0UVfQaZoirA/yVtvVF3mv5Nh/RfCvCvGGR2s\n"
            + "PNgnq7ZKia5/nXqV4/l2Yrrg2QPYtAYwuv2Sf6GK5MqhqBtWb8qZRZVH7df4IroJ\n"
            + "orWmi2obRqdU5+w2F+OaL7OOs8GIwLb9iGeswwIDAQABAoIEAQC9ZPnoLgEeMyNs\n"
            + "DWM+GbfozXYt9OqEs/VwJLvOx9GLaUgcgQWsRw7Ai1oVzCZz6e4mOl2fRQWzMLCb\n"
            + "YEaoAk6HvEtEh7vSX1cs3dlA0Thu09pzSOTc16+Tf7pEn02dM6btFqmpTwBn8tTF\n"
            + "M9sC5oWFhB4aQHkETEJwY9B5TMtSCTa80rKiAOZlhYaqBzFDLby5VAcAk/DiKQ7z\n"
            + "HUt0ssHdmPZKC/7++GG3IFjpR99pqf5JoniB55v/4Wib4DoT1p5ZCz3Dg5Ztek2Y\n"
            + "+aH1n6wSzqbKfo/kRkp+cJ4jEv7YLjVOlz/zNeitkhfMfbaRMv3jkn44kIzkHD5r\n"
            + "wqJmooPHkV/UbT1lOMU5iiGV+V2ue+M3WDYBPKLU1aorABQ71O0EUSKOcRcAOIP2\n"
            + "p2UADoeWP0NqwfSLVtk7WK+8LTfe9RhC9lbqg5vZW1fkRFH6QYwoZVrTv3FkiZ22\n"
            + "eqQsL5GK5O8RENxHp9ShtpdrerfOGW+mIV680BWR+fk06sbWLqpC9prgQzmcM+vX\n"
            + "4WpJ3AzL2TbZNlvkvMDKpIgKuQHRJkqAF2HIGcO9nrOHK4vpPAEZkd9oHSi4qNwF\n"
            + "LDewi52xvwKd3CDHM+GYTU7giJqZ7JantAEYEVEWs+JRpSkf7CbX/d7NrEci3gyA\n"
            + "hj04u0sbiSZdf/TDhAjaH0VFv5Qk+xEf4NbGvL+UZse/WYCINYQNSbLZCH3ZmnFn\n"
            + "3JG/vlAB1ojpAHJb2Eg2zTIcPw//ocig27ZvzZG1fwebLB0dwPoMOtaeh66hzSv/\n"
            + "TTduDJsiLg/x/I9Fbw/uJd6DSypndSCq+BNUy3g32umnsmj4x3XTnfn56d2E5n2w\n"
            + "ivMxYaFlyMTJ3ilJJEpk2ioLzlWYVhZFMielmrpsE8EM4Rnv+lVfPzu7VDSIyMDI\n"
            + "L6VPLRG4+wakSajacwKBGfVB/wEWrhGxQb9uHdFcXbzNmx2m/70S5LEiOTmTv47g\n"
            + "rSD7bPxQ9ghx9XcXnoFjts+Crz9Pl0NbmMCJQ2KTPmAXCMJNBC0Xof3yrP0+ZKM+\n"
            + "ZNXTAhCPesDTgOmYtMnyKZa0XxzCf5DgBYa6zzT484w0qZcMRYewFLJEv0oD2cwC\n"
            + "WSbjvvCmBJLHxp9+L3H5uE4hMLuZhdIV7+KrwTetylRNxjM7wi8w0uUUE9/6uHK4\n"
            + "Wy4DA7kfjPmvGhbbv/63baVQRRCs7M8Sk6rUD+JZ+ZKzVK31+j8WwNT7zbXte2qs\n"
            + "NCnEPrGvJRKb3arq6VMJXzLlknFUbOiO6S8EvgeglunPtqyadMIhNQdk7cW+4hzq\n"
            + "tY4aNtT4zdozmji/WzDBSPMhIYh0CV7zSgVuFS/SsWyAWDHX4IvZCoHLlg+bROaE\n"
            + "NdnX6B6JAoICAQD9V6GdwcUsgbH0jJb3dhQ0Zpqcrrl8U496msYkg+b6MyqX55Ev\n"
            + "YItv16grvhl7QupK93pD256zGm+83e6Jxox4ZxLLmgx6fH3ubkRC7CbpRWtb7R1o\n"
            + "3YENjo8wmbjjcs38tUG5LEHFsZOeyVWkkW+/PCuOUl4m/3m7BOiwEYIA6vYdC42F\n"
            + "kBH8FaRi2zZ+kiHX2vHIbdpb7H013gSZt8ieKrLJh6FESFSINuW4ISGbCgn9nEw7\n"
            + "uAG7EMwQliCe66SIx+aYxaaxJL0q0tJkX/glzO//9Fu5LX+n5nCgqcTv2TB5IANi\n"
            + "fyi/YrbaX1hUxJFYyh39a1rIpjk4wSZwXZIC6ZHc3CKu5mNhML4a809W5siKXR/9\n"
            + "hQqiSeCIXuBq96E+s9kGNp9hLBUgQGooZwMZdVgOX0dSf4E6KoVz5c4PTEuJO7K8\n"
            + "wxlTfegUxzKzYi+A29lNHiYJF51CSJGr7Z1VFWpVUa7Ts953SgsfIYqB1Yrflgtn\n"
            + "vJi3+FDuNLPIMeVJLDdp8tXBUDnDzZL4eRdQEIzmYJuExEj3g3i8Vmd1fJ5yhtEd\n"
            + "7N93KVstqp8mGvYYnfCfXZUiwn1ivRgzfeYR+siTKuTXmwhsXBcy6Z5cLZOatjUC\n"
            + "uBME4We4ra9AoXc6iU9Zrn8zAEMeEtl8QSiIk2OJkqTIaWck1Li6RpRJVwKCAgEA\n"
            + "6qM8dzcv6naHOSJmbdZT8c8yVVAEbhkgj22mdfHJa437o0dqXm8oKyDhucDhG1kx\n"
            + "Uj4lBlx7vH5uDJJ3YbrsLQcQhf1r7ol84VAlJmm9OF6horLKRWc2Eke8LbtRw3VV\n"
            + "9M9fOP4b5gwJFeVOYHK61iKRwDw+qs1GX/5jOQLPdDC+BSzLFCGvDCFk6BrAadcn\n"
            + "IVfJ+7Sid9v30kpUPGpDiJzZ0Ofb6+1ppcW/YlZE6OPBJMD0cIcd3ogMFPBA+ZuQ\n"
            + "zLPf3PVbEUxDNLOCjTvLie5jvGvBTXrGSvXXTRt+rpBkkrd6GqM3jKBEOPMxWiFm\n"
            + "Uiw8WqOKKC46FpiUAD82uJddnd1R/dM1cbtJiF3QX5CEGbaKdAWzoG0CODw5tyX1\n"
            + "Cxgy43PCQafz/AOKAecNp6eCWkyEbjCBsCrJg3lKfQWyJ0MqToadSA+iDUYRA6PB\n"
            + "sEJWiy8UtnbwgDtTtUNKxWhbGOLxGRQnnSo7USE93ew8tuWa9oW9S/BEF7AiCk+S\n"
            + "HCINMB7ek2QXzFqS3h7WBSzDn+ZsSOo3EH5I5NV0EfgeGfAnC9mKLc9wM6XiuBOY\n"
            + "eLiFPc6nLlB8jJpEFioK44oQl4qV8NCP4Dwz2r+iLUt8E8vQAypYNZrZTLEktIL9\n"
            + "Egl+3+bUPgMJOVqwI4YCkICnJjnlVDkBL2xpFmsOGHUCggIAEvUDuvJM9s+dqVb7\n"
            + "1PiY+nLTDvZkGtGF4v7B5OmZ1w8NGODTFGB9DplslBldfsO7FHEATSOZ9Hz973wL\n"
            + "5XNd/4R2+5VDacb3BWhq4zcYkkwHhJFxqe8pQQJx5IkcNKjakRZfHKQbJ9fp2+/k\n"
            + "4LOhUQYHnFa9hN2JFl1/q+0jdT4fvHyo0l29eseDzYHpyf7VWXmgrgbKWCaSF/3N\n"
            + "ClOeR3eaeUoU3y8qZCb3eZfBFADkTn3rlmxmdMEFBBi3yCyJ21JaBwSDPK4rGZE8\n"
            + "/RXRU8LKErUOSAUHkGDF/L+3ZNszrVyf5DbvraKNXDnWOkGbPrGhHN1zpaAKmByb\n"
            + "67yUuHMR3xz522yR8yvajdm3DiGmz/O3+RiDezFcA9hVoqt0/WQn0Tc1JehOjGNF\n"
            + "jlBnAvis5iZrB9lSqi+UXN/NU4e5/0LgVQ+kTYMWYrelK5clRtcso4CmB/gkZFlZ\n"
            + "zSuyojNACbJbCqxi8ToxKtsvqhd4lNJ9d/28z8ddBvYandhd9+O/IcZyCE0ghW5U\n"
            + "mRM2k18pq/N+r6igbSUBW9Z7V2dD0/4Sl9KpxhjqIbiqwAc0cxMedk5iYn97MnBD\n"
            + "51Z8aMwDRj/nb9rB/pnFgqHIn80pRmJsBRARHERhpogYnRV3/oFX1rYf/oj+fLmc\n"
            + "XJfjmJSu1hSLEBQTC8Z/LDEr13ECggIBAOlnB7bvRtLMpSbIeWu5UDeyDDehKUb7\n"
            + "58/FG1kn81zyF+cMG1tk52g/hUrp+wLhbpaJCvuQ8+VFPuNyrx6gel8wL9eZh8v5\n"
            + "KChZORtFA90XBWJ6x4rSaI82nJJBS8xK4/5qaiafX9EvF7qYJ6b5ebGZIbNAOnZd\n"
            + "TCwhOUJ08Th7ZApxzHFyMFa4wU/BjLW8OEiKs3mW7iacwaCGH9UZP6Sdom6Utcey\n"
            + "mu00EHUZq+Ke7HpLFtz5C1VZr+sEMx4ZCakXJRD/YF+MpS2/g5ZKbOYAJWZBKkCQ\n"
            + "aMAYXNtvBk1PhTwNF4F36sIQisy73dPydX44UrE3DS97DH19uXulZiGpMI7gobcE\n"
            + "ap1/2F22NJlbgIyzcHaJVW24AgU+o4r0TxWCNNzdQddd4u5F9vp9hK/JiXmZtAKI\n"
            + "bfl4FoyaEubay6USwvrqHXqZUnIxyKr+MqXK15wMcWYwWny0h0hAcBh+/l97IKn5\n"
            + "yo4kfGzvzEL9xEeLjuK7ltn7X0DRDIuFK6qglM3RZ0bmwmWdk4sw0WTEarSc2gqO\n"
            + "MchOVuSLELLvRcI3ih/XfgSj3NEDqsvBcmJj6ubYsqT3m22h5yjFGZ/Or0KPsSej\n"
            + "z/sW594p0oGMHRj0HS+I58YrCw2nCQQnaOaQW40OaQJmsr5C4AP2QobL83mrDd0B\n"
            + "95PdG4wZYiQhAoICAAEQjP1dbHOT/4oycWukZIayEN3rGJbwjLJcM5n+VVMMprDE\n"
            + "2Bx9YmjijOMRHqCZY7rDr6OMhnuI1E2gkAbTfHfLWM3OpnagdL73uAjYWoU8QIZl\n"
            + "8NS3u8lNbDQby4WIbmtvIbPbKrnV47illLfdfcMvp16E+zXpgBERZfA5kjdvL2Zs\n"
            + "CoKitgJmes6Cw7gpxq04wuuq5xJ856rPmwiFTJCJGoN5yXQYZsqNsz1iWvjTVUen\n"
            + "JjNj5aveSb1a34JEqlLxk2NoYrT47A54iMJQSqXQQDwbW2U0vOZMv9FtwjJti9EO\n"
            + "aIma8wcoQhLGqt9/yQ66BzLCCQfnxMDgGUK7RD85Jcq7WghgSJv8iRQ4Hu8J7tTO\n"
            + "SZJOJy9IDmSp/Yh5R/9KMEdgvUQRo4JFqORyguokkKTa2EYYPpoDfCv1uEg8rr+4\n"
            + "xtKBxoiw+UJZGnEbUGJw2Yt2ynbV4dUlUkgMOBrznYdtmivaF544DD8Tgk8elNdT\n"
            + "WowvdHlPa9HCY7VNWo80vaOUXUSo6ftUkTVhyd9EI84g1JyFGPcpd624NsAhQrSm\n"
            + "kg8qubbQM3Tij5oGYewJ2PzeweosiQ4AVlq3+mVGnkNLPLyABHOgGmoUPpyR297R\n"
            + "nDe3iQJfqKGHQck94dCtx7mlbcgBwWiM1SCdZcJ8H06gBWA99hNqcwWS9kSv\n"
            + "-----END RSA PRIVATE KEY-----\n";

    private static final String IDS_RSA_PUB = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAEAQDoM6Rcnsrz0CLcT5dO8WN582ZdGthUjHHxMWidcZ9S9J00hIO5lb60L0VsU0ElZlQWW51B0cBzMsIos6hSruZMF7/A86t5LIgMjWI2drBX8qxfs6TJOGwOiUrGBED7B7wm5RgB6zbL3gUprDiQBj85vXpZJ/sA/1jw5rj89wrooIiTNDGKov5IYbeYvL5eFNT+klicQLxzWXD4ZqHyg9YbW/HlbnvyJaLGBNs2nRNNc2vX6NTFVGDVKK0LhY/yDrtp/p/muFwlm3kPP+BZqMeoqQr3ueY9IO22yjnq555RbcvyUzexwWMZC7dBukuYWkxl5Ol486fsKNWN8Fmehw3MN2qSAT6K8QHudZY1uATAAtfg1HgPOQYagU9Qp2wh0QELkPvdcLo0nEcpY470v4rJTGh4DyFQcG5ZzelVKLs3IbTg++oR87QRrWnhOfadvYKt4SA7IJD+cskms8QN5/7wZQoqze1VrBs6fq0sXKLtoVj27fkpfd+C3m3xOIiqZwjGzugLXqfL/3ul7uAFFm4tTgDErvqbo3GvWUbDvAk5eWWGgptnKzrvqqUgbpW9LD90BR6Rj9WT124ib0EcctVl62/8sviFY3LvEy+WmXxXXjVrMnAETXRXUa68mdG2x/ntmpI6OJ7NN7Bq+14kvBNmVFNXtDCxWlQYztjJglpy/UaQXPPTRRaD789/WDIsd/4mCxbHV+1RT1aWdWAFJ9mlYod7GNwqsnOTif3odYUviKEJ/F3z3lmQGxtPK5ECwLdPD6T8ULqPRtszl4T3B4Ef1vcCnq1kWUjATBcK9TbyLRJvESo9emfGkrfddEfPdvK7xC1fgG6VWvBrQ/9l5jR++ihQmj61SodsTs9/kOXPKVlqtdAFIeM9wb5uzfssDZqv/WjqFFcnlndvx/hgfce6Ya6Jhjc2MO+r74/lFOdqxYU3v5/XFh7EHcdkT1FfEJNNUifT19UedqWg0Ke+GXp+fjRN+yZLXSo3WHCHnmfO+lSOobOrIL71n8X5GmoggBUrKKPIzLY+IlEHLyw9lrWp/37EMJmxFh1Q95uvy3fyw8Rhi1HzAx9jWKrSpefU+L8b6CGZOf79Qh25uf+bsi78XQZLI3pyPPh+ySlLlsBvWgmGhYPKvHoQB/3o6hUisxBxoGQtVx3sxbn2tIMrSA0rsMTdldrzlXBwyP9j0cLJtj9eV/m/sKyLfwT7Jsc/ZHdgQ8x24uDeGuzRRV9BpmiKsD/JW29UXea/k2H9F8K8K8YZHaw82CertkqJrn+depXj+XZiuuDZA9i0BjC6/ZJ/oYrkyqGoG1ZvyplFlUft1/giugmitaaLahtGp1Tn7DYX45ovs46zwYjAtv2IZ6zD tester";

    private static File kojiDb;
    private static File sources;
    private static File secondDir;
    private static File priv;
    private static File pub;
    private static int port;
    private static SshApiService server;
    private static final boolean debug = true;

    @BeforeClass
    public static void startSshdServer() throws IOException, GeneralSecurityException {
        ServerSocket s = new ServerSocket(0);
        port = s.getLocalPort();
        final File keys = File.createTempFile("ssh-fake-koji.", ".TestKeys");
        keys.delete();
        keys.mkdir();
        priv = new File(keys, "id_rsa");
        pub = new File(keys, "id_rsa.pub");
        createFile(priv, IDS_RSA);
        createFile(pub, IDS_RSA_PUB);
        Set<PosixFilePermission> perms = new HashSet<>();
        Files.setPosixFilePermissions(pub.toPath(), perms);
        Files.setPosixFilePermissions(priv.toPath(), perms);
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        Files.setPosixFilePermissions(pub.toPath(), perms);
        Files.setPosixFilePermissions(priv.toPath(), perms);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                priv.delete();
                pub.delete();
                keys.delete();
            }
        });
        s.close();
        kojiDb = File.createTempFile("ssh-fake-koji.", ".root");
        kojiDb.delete();
        kojiDb.mkdir();
        kojiDb.deleteOnExit();
        server = new SshApiService(kojiDb, port, "tester=" + pub.getAbsolutePath());
        server.start();
        sources = File.createTempFile("ssh-fake-koji.", ".sources");
        sources.delete();
        sources.mkdir();
        sources.deleteOnExit();
        secondDir = File.createTempFile("ssh-fake-koji.", ".secondDir");
        secondDir.delete();
        secondDir.mkdir();
        secondDir.deleteOnExit();
    }

    @AfterClass
    public static void stopSshd() throws IOException {
        server.stop();
    }

    private static void createFile(File path, String content) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            bw.write(content);
        }
    }

    private static String readFile(File path) {
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                return br.readLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "impossible";
        }
    }

    private static int scpTo(String target, String... source) throws InterruptedException, IOException {
        return scpTo(new String[0], target, null, source);
    }

    private static int scpToCwd(String target, File cwd, String... source) throws InterruptedException, IOException {
        return scpTo(new String[0], target, cwd, source);
    }

    private static final String TESTERatLOCALHOSTscp = "tester@localhost:";
    private static final String[] RECURSIVE = new String[]{"-r"};

    private static int scpTo(String[] params, String target, File cwd, String... source) throws InterruptedException, IOException {
        String fullTarget = TESTERatLOCALHOSTscp + target;
        return scpRaw(params, fullTarget, cwd, source);
    }

    private static int scpFrom(String target, String... source) throws InterruptedException, IOException {
        return scpFrom(new String[0], target, null, source);
    }

    private static int scpFromCwd(String target, File cwd, String... source) throws InterruptedException, IOException {
        return scpFrom(new String[0], target, cwd, source);
    }

    private static int scpFrom(String[] params, String target, File cwd, String... source) throws InterruptedException, IOException {
        for (int i = 0; i < source.length; i++) {
            source[i] = TESTERatLOCALHOSTscp + source[i];
        }
        return scpRaw(params, target, cwd, source);
    }

    private static int scpRaw(String[] params, String target, File cwd, String... source) throws InterruptedException, IOException {
        title(3);
        List<String> cmd = new ArrayList<>(params.length + source.length + 9);
        cmd.add("scp");
        //cmd.add("-v"); //verbose 
        cmd.add("-o");
        cmd.add("StrictHostKeyChecking=no");
        cmd.add("-i");
        cmd.add(priv.getAbsolutePath());
        cmd.add("-P");
        cmd.add("" + port);
        cmd.addAll(Arrays.asList(params));
        cmd.addAll(Arrays.asList(source));
        cmd.add(target);
        if (debug) {
            for (int i = 0; i < params.length; i++) {
                String param = params[i];
                System.out.println(i + ". param: " + param);

            }
            for (int i = 0; i < source.length; i++) {
                String string = source[i];
                System.out.println(i + ". scp from " + string);
                System.out.println("   scp to   " + target);
            }
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (cwd != null) {
            pb.directory(cwd);
        }
        Process p = pb.start();
        if (debug) {
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (true) {
                String s = br.readLine();
                if (s == null) {
                    break;
                }
                System.out.println(s);
            }
        }
        int i = p.waitFor();
        if (debug) {
            System.out.println(" === scpEnd === ");
        }
        return i;
    }

    @After
    public void cleanSecondDir() {
        clean(secondDir);
    }

    @After
    public void cleanSources() {
        clean(sources);
    }

    @After
    public void cleanKojiDb() {
        clean(kojiDb);
    }

    private void clean(File f) {
        File[] content = f.listFiles();
        for (File file : content) {
            deleteRecursively(file);
        }
        Assert.assertTrue(f.isDirectory());
        Assert.assertTrue(f.listFiles().length == 0);
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] content = file.listFiles();
            for (File f : content) {
                deleteRecursively(f);
            }
        }
        file.delete();
    }

    private static void checkFileExists(File f) {
        if (debug) {
            System.out.println(f + " is supposed to exists. f.exists() is: " + f.exists());
            if (f.exists()) {
                System.out.println("content: '" + readFile(f) + "'");
            }
        }
        Assert.assertTrue(f + " was supposed to exists. was not", f.exists());
    }

    private static void checkFileNotExists(File f) {
        if (debug) {
            System.out.println(f + " is supposed to NOT exists. f.exists() is: " + f.exists());
            if (f.exists()) {
                System.out.println("content: '" + readFile(f) + "'");
            }
        }
        Assert.assertFalse(f + " was supposed to NOT exists. was", f.exists());
    }

    private static void title(int i) {
        if (debug) {
            String s = "method-unknow";
            if (Thread.currentThread().getStackTrace().length > i) {
                s = Thread.currentThread().getStackTrace()[i].getMethodName();
            }
            System.out.println(" ==" + i + "== " + s + " ==" + i + "== ");
        }
    }

    private class NvraTarballPathsHelper {

        private final String vid;
        private final String rid;
        private final String aid;

        public NvraTarballPathsHelper(String id) {
            this(id, id, id);
        }

        public NvraTarballPathsHelper(String vid, String rid, String aid) {
            this.vid = vid;
            this.rid = rid;
            this.aid = aid;
        }

        public String getName() {
            return "terrible-x-name-version" + vid + "-release" + rid + ".arch" + aid + ".suffix";
        }

        public String getLocalName() {
            return getName();
        }

        public String getRemoteName() {
            return getName();
        }

        public String getNVRstub() {
            return "terrible-x-name/version" + vid + "/release" + rid;
        }

        public String getStub() {
            return getNVRstub() + "/" + getArch();
        }

        protected String contentKey() {
            return "nvra";
        }

        public String getContent() {
            return contentKey() + " - " + vid + ":" + rid + ":" + ":" + aid;
        }

        public String getStubWithName() {
            return getStub() + "/" + getRemoteName();
        }

        public File getLocalFile() {
            return new File(sources, getLocalName());
        }

        public File getSecondaryLocalFile() {
            return new File(secondDir, getLocalName());
        }

        public void createLocal() throws IOException {
            createFile(getLocalFile(), getContent());
            checkFileExists(getLocalFile());
        }

        public void createSecondaryLocal() throws IOException {
            createFile(getSecondaryLocalFile(), getContent());
            checkFileExists(getSecondaryLocalFile());
        }

        public void createRemote() throws IOException {
            getRemoteFile().getParentFile().mkdirs();
            createFile(getRemoteFile(), getContent());
            checkFileExists(getRemoteFile());
        }

        public File getRemoteFile() {
            return new File(kojiDb, getStubWithName());
        }

        public String getArch() {
            return "arch" + aid;
        }

    }

    @Test
    /*
     * scp /abs/path/nvra tester@localhost:
     */
    public void scpNvraAbsPathsTo() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("1t1");
        nvra.createLocal();
        int r = scpTo("", nvra.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /*
     *scp tester@localhost:nvra /abs/path/
     */
    public void scpNvraAbsPathsFrom1() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("1f1");
        nvra.createRemote();
        int r2 = scpFrom(nvra.getLocalFile().getParent(), nvra.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getLocalFile());

    }

    @Test
    /*
     *scp tester@localhost:/nvra /abs/path
     */
    public void scpNvraAbsPathsFrom2() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("1f2");
        nvra.createRemote();
        int r3 = scpFrom(nvra.getLocalFile().getParent(), "/" + nvra.getName());
        Assert.assertTrue(r3 == 0);
        checkFileExists(nvra.getLocalFile());
    }

    @Test
    /*
     * scp /abs/path/nvra tester@localhost:/nvra
     */
    public void scpNvraAbsPathsRenameLikeTo() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("2t1");
        nvra.createLocal();
        int r = scpTo("/" + nvra.getName(), nvra.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /*
     * scp tester@localhost:nvra /abs/path/nvra
     */
    public void scpNvraAbsPathsRenameLikeFrom1() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("2t1");
        nvra.createRemote();
        int r1 = scpFrom(nvra.getLocalFile().getAbsolutePath(), nvra.getName());
        Assert.assertTrue(r1 == 0);
        checkFileExists(nvra.getLocalFile());
    }

    @Test
    /*
     * scp tester@localhost:nvra /abs/path/nvra2
     */
    public void scpNvraAbsPathsRenameLikeFrom2() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("2t2");
        nvra.createRemote();
        int r2 = scpFrom(new File(nvra.getLocalFile().getParent(), "nvra2").getAbsolutePath(), nvra.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(new File(nvra.getLocalFile().getParent(), "nvra2"));
    }

    @Test
    /*
     * scp nvra tester@localhost:
     */
    public void scpNvraAllRelativeToNoTarget() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("3t1");
        nvra.createLocal();
        int r = scpToCwd("", sources, nvra.getLocalFile().getName());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /*
     * scp nvra tester@localhost:nvra
     */
    public void scpNvraAllRelativeTo() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("3t1");
        nvra.createLocal();
        int r = scpToCwd("", sources, nvra.getLocalFile().getName());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /*
     * scp nvra tester@localhost:/nvra
     */
    public void scpNvraAllRelativeToPseudoAbs() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("3t2");
        nvra.createLocal();
        int r = scpToCwd("/", sources, nvra.getLocalFile().getName());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /*
     * scp tester@localhost:nvra .
     */
    public void scpNvraAbsPathsFromNameToCwd() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("3f1");
        nvra.createRemote();
        int r2 = scpFromCwd(".", sources, nvra.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getLocalFile());

    }

    @Test
    /*
     * scp tester@localhost:nvra ./nvra2
     */
    public void scpNvraAbsPathsFromNameToCwdrenamed() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("3f2");
        nvra.createRemote();
        int r2 = scpFromCwd("./renamed", sources, nvra.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(new File(nvra.getLocalFile().getParentFile(), "renamed"));

    }

    @Test
    /*
      * scp /abs/path/nvra tester@localhost:/some/garbage
     */
    public void scpNvraAbsToAbsGarbage() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("4t1");
        nvra.createLocal();
        int r = scpTo("/some/garbage", nvra.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /*
      scp /abs/path/nvra tester@localhost:/some/garbage/nvra
     */
    public void scpNvraAbsToAbsGarbageRenameLike() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("4t2");
        nvra.createLocal();
        int r = scpTo("/some/garbage/" + nvra.getName(), nvra.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /*
     * scp nvra tester@localhost:some/garbage
     */
    public void scpNvraRelToRelGarbage() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("4t3");
        nvra.createLocal();
        int r = scpToCwd("some/garbage/" + nvra.getName(), nvra.getLocalFile().getParentFile(), nvra.getName());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /* scp nvra tester@localhost:some/garbage/nvra
     */
    public void scpNvraRelToRelGarbageRenameLike() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("4t3");
        nvra.createLocal();
        int r = scpToCwd("some/garbage/" + nvra.getName(), nvra.getLocalFile().getParentFile(), nvra.getName());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /*
     * scp tester@localhost:/some/garbage/nvra /abs/path/
     */
    public void scpAbsGarbagedNvraToAbs() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("4f1");
        nvra.createRemote();
        int r2 = scpFrom(nvra.getLocalFile().getAbsolutePath(), "/some/garbage/" + nvra.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getLocalFile());

    }

    @Test
    /*
     * scp tester@localhost:some/garbage/nvra some/path/
     */
    public void scpRelGarbagedNvraToRel() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("4f2");
        nvra.createRemote();
        int r2 = scpFromCwd(nvra.getName(), sources, "some/garbage/" + nvra.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getLocalFile());

    }

    @Test
    /* scp  /some/path/nvra2 tester@localhost:some/garbage/nvra1
     */
    public void scpNvraAbsToAbsGarbageRenameLikeAnotherNvraRel() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvraSource = new NvraTarballPathsHelper("5t1x1");
        NvraTarballPathsHelper nvraTarget = new NvraTarballPathsHelper("5t1x2");
        nvraSource.createLocal();
        checkFileNotExists(nvraTarget.getLocalFile());
        int r = scpTo("some/garbage/" + nvraTarget.getName(), nvraSource.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvraTarget.getRemoteFile());
        checkFileNotExists(nvraSource.getRemoteFile());
    }

    @Test
    /* scp  /some/path/nvra2 tester@localhost:/some/garbage/nvra1
     */
    public void scpNvraAbsToAbsGarbageRenameLikeAnotherNvraAbs() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvraSource = new NvraTarballPathsHelper("5t2x1");
        NvraTarballPathsHelper nvraTarget = new NvraTarballPathsHelper("5t2x2");
        nvraSource.createLocal();
        checkFileNotExists(nvraTarget.getLocalFile());
        int r = scpTo("/some/garbage/" + nvraTarget.getName(), nvraSource.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvraTarget.getRemoteFile());
        checkFileNotExists(nvraSource.getRemoteFile());
    }

    @Test

    /*
     * scp tester@localhost:some/garbage/nvra1 /some/path/nvra2
     */
    public void scpNvrFromAbsGarbageRenameLikeAnotherNvraAbs() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvraSource = new NvraTarballPathsHelper("5t2x1");
        NvraTarballPathsHelper nvraTarget = new NvraTarballPathsHelper("5t2x2");
        nvraSource.createRemote();
        checkFileNotExists(nvraTarget.getRemoteFile());
        int r = scpFrom(nvraTarget.getLocalFile().getAbsolutePath(), "/some/garbage/" + nvraSource.getName());
        Assert.assertTrue(r == 0);
        checkFileExists(nvraTarget.getLocalFile());
        checkFileNotExists(nvraSource.getLocalFile());
    }

    @Test
    /*
     * scp  some/path/nonNvra tester@localhost:some/garbage/nvra
     */
    public void scpSomeFileToGarbagedNvra() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("5t3");
        nvra.createLocal();
        File renamed = new File(nvra.getLocalFile().getParentFile(), "renamed");
        nvra.getLocalFile().renameTo(renamed);
        checkFileNotExists(nvra.getLocalFile());
        checkFileExists(renamed);
        nvra.getLocalFile().renameTo(renamed);
        int r = scpTo("some/garbage/" + nvra.getName(), renamed.getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    /*
     * scp tester@localhost:some/garbage/nvra /some/path/nonNvra
     */
    public void scpSomeFileFromGarbagedNvra() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("5f3");
        nvra.createRemote();
        File nwFile = new File(nvra.getLocalFile().getParent(), "renamed");
        int r = scpFrom(nwFile.getAbsolutePath(), "some/garbage/" + nvra.getName());
        Assert.assertTrue(r == 0);
        checkFileNotExists(nvra.getLocalFile());
        checkFileExists(nwFile);
    }

    @Test
    /*
     * scp  some/path/nvra tester@localhost:some/garbage/NonNvra
     */
    public void scpNvraToMoreGarbaged() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("6t2");
        nvra.createLocal();
        int r2 = scpTo("/some/garbage/someFile", nvra.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getRemoteFile());

    }

    @Test
    /*
     * scp tester@localhost:some/garbage/nonNvra some/path/nvra
     */
    public void scpNonsenseToNvra() throws IOException, InterruptedException {
        //inccorect case
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("6f1");
        nvra.createRemote();
        int r = scpFrom(nvra.getLocalFile().getAbsolutePath(), "some/garbage/notExisting");
        Assert.assertTrue(r != 0);
        checkFileNotExists(nvra.getLocalFile());
    }

    @Test
    /*
     * scp  some/path/nvra1 some/path/nvra1 tester@localhost:
     */
    public void scpMultipleFilesToRelNothing() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra1 = new NvraTarballPathsHelper("7t3");
        NvraTarballPathsHelper nvra2 = new NvraTarballPathsHelper("8t3");
        nvra1.createLocal();
        nvra2.createLocal();
        int r = scpTo("", nvra1.getLocalFile().getAbsolutePath(), nvra2.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra1.getRemoteFile());
        checkFileExists(nvra2.getRemoteFile());
    }

    @Test
    /*
     * scp  some/path/nvra1 some/path/nvra1 tester@localhost:
     */
    public void scpMultipleFilesToAbsGarbage() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra1 = new NvraTarballPathsHelper("7t4");
        NvraTarballPathsHelper nvra2 = new NvraTarballPathsHelper("8t4");
        nvra1.createLocal();
        nvra2.createLocal();
        int r = scpTo("/some/path/", nvra1.getLocalFile().getAbsolutePath(), nvra2.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra1.getRemoteFile());
        checkFileExists(nvra2.getRemoteFile());
    }

    @Test
    /*
     * scp  tester@localhost:/seome/garbage/nvra1 tester@localhost:/another/garbage/nvra2 .
     */
    public void scpMultipleFilesFromAbsGarbage() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra1 = new NvraTarballPathsHelper("7f3");
        NvraTarballPathsHelper nvra2 = new NvraTarballPathsHelper("8f3");
        nvra1.createRemote();
        nvra2.createRemote();
        int r = scpFrom(sources.getAbsolutePath(), "some/garbage/" + nvra1.getName(), "/another/garbage/" + nvra2.getName());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra1.getLocalFile());
        checkFileExists(nvra2.getLocalFile());
    }

    @Test
    /*
     * scp  tester@localhost:nvra1 tester@localhost:nvra2 dir
     */
    public void scpMultipleFilesFrom() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra1 = new NvraTarballPathsHelper("7f4");
        NvraTarballPathsHelper nvra2 = new NvraTarballPathsHelper("8f4");
        nvra1.createRemote();
        nvra2.createRemote();
        int r = scpFrom(sources.getAbsolutePath(), nvra1.getName(), nvra2.getName());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra1.getLocalFile());
        checkFileExists(nvra2.getLocalFile());
    }

    /*
    DATA
     */
    private class NvraDataPathsHelper extends NvraTarballPathsHelper {

        private final String localName;
        private final String remoteName;

        @Override
        protected String contentKey() {
            return "data";
        }

        public NvraDataPathsHelper(String id, String name) {
            this(id, name, name);
        }

        public NvraDataPathsHelper(String id, String localName, String remoteName) {
            super(id);
            this.localName = localName;
            this.remoteName = remoteName;
        }

        public NvraDataPathsHelper(String vid, String rid, String aid, String name) {
            this(vid, rid, aid, name, name);
        }

        public NvraDataPathsHelper(String vid, String rid, String aid, String localName, String remoteName) {
            super(vid, rid, aid);
            this.localName = localName;
            this.remoteName = remoteName;
        }

        @Override
        public String getRemoteName() {
            return remoteName;
        }

        @Override
        public String getLocalName() {
            return localName;
        }

        @Override
        public String getStub() {
            //no arch!
            return super.getNVRstub() + "/data";
        }

    }

    // scp  /some/path/logname tester@localhost:nvra/data
    @Test
    public void scpDataToRelativeNvraDataNoSlash() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data1To", "dataFile");
        nvraDataFile.createLocal();
        int r2 = scpTo(nvraDataFile.getName() + "/data", nvraDataFile.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvraDataFile.getRemoteFile());
    }

    // scp  /some/path/logname tester@localhost:nvra/data/
    @Test
    public void scpDataToRelativeNvraDataSlash() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data2To", "dataFile");
        nvraDataFile.createLocal();
        int r2 = scpTo(nvraDataFile.getName() + "/data/", nvraDataFile.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvraDataFile.getRemoteFile());

    }

    // scp  /some/path/logname tester@localhost:some/garbage/nvra/data
    @Test
    public void scpDataToRelGarbageNvraDataNoSlash() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data3To", "dataFile");
        nvraDataFile.createLocal();
        int r2 = scpTo("some/garbage/" + nvraDataFile.getName() + "/data", nvraDataFile.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvraDataFile.getRemoteFile());
    }

    // scp  /some/path/logname tester@localhost:some/garbage/nvra/data/
    @Test
    public void scpDataToRelGarbageNvraDataSlash() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data4To", "dataFile");
        nvraDataFile.createLocal();
        int r2 = scpTo("some/garbage/" + nvraDataFile.getName() + "/data/", nvraDataFile.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvraDataFile.getRemoteFile());
    }

    // scp  /some/path/logname tester@localhost:/some/garbage/nvra/data
    @Test
    public void scpDataToAbsGarbageNvraDataNoSlash() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data5To", "dataFile");
        nvraDataFile.createLocal();
        int r2 = scpTo("/some/garbage/" + nvraDataFile.getName() + "/data", nvraDataFile.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvraDataFile.getRemoteFile());
    }

    // scp  /some/path/logname tester@localhost:nvra/data/renamedLog
    @Test
    public void scpDataToRelNvraDataRenamed() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data6To", "dataFile", "newName");
        nvraDataFile.createLocal();
        int r2 = scpTo(nvraDataFile.getName() + "/data/newName", nvraDataFile.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvraDataFile.getRemoteFile());
    }

    // scp  /some/path/logname tester@localhost:some/garbage/nvra/data/renamedLog
    @Test
    public void scpDataToRelGarbageNvraDataRenamed() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data7To", "dataFile", "newName");
        nvraDataFile.createLocal();
        int r2 = scpTo("some/garabge/" + nvraDataFile.getName() + "/data/newName", nvraDataFile.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvraDataFile.getRemoteFile());
    }

    // scp  /some/path/logname tester@localhost:/some/garbage/nvra/data/renamedLog
    @Test
    public void scpDataToAbsGarbageNvraDataRenamed() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data8To", "dataFile", "newName");
        nvraDataFile.createLocal();
        int r2 = scpTo("/some/garabge/" + nvraDataFile.getName() + "/data/newName", nvraDataFile.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvraDataFile.getRemoteFile());
    }

    // scp  /some/path/logname tester@localhost:data
    @Test
    public void scpDataToRelDataWrong() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data9To", "dataFile");
        nvraDataFile.createLocal();
        int r2 = scpTo("data", nvraDataFile.getLocalFile().getAbsolutePath());
        Assert.assertFalse(r2 == 0);
        checkFileNotExists(nvraDataFile.getRemoteFile());
    }

    // scp  /some/path/logname tester@localhost:/
    @Test
    public void scpDataToNothing() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data10To", "dataFile", "newName");
        nvraDataFile.createLocal();
        int r2 = scpTo("/", nvraDataFile.getLocalFile().getAbsolutePath());
        Assert.assertFalse(r2 == 0);
        checkFileNotExists(nvraDataFile.getRemoteFile());
    }

    // scp  tester@localhost:nvra/data/name /some/path/
    @Test
    public void scpDataFromRelToDir() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data1f1", "dataFile");
        nvraDataFile.createRemote();
        int r2 = scpFrom(sources.getAbsolutePath(), nvraDataFile.getName() + "/data/" + nvraDataFile.getRemoteName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvraDataFile.getLocalFile());
    }

    // scp  tester@localhost:/nvra/data/name /some/path/
    @Test
    public void scpDataFromAbsToDir() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data2f1", "dataFile");
        nvraDataFile.createRemote();
        int r2 = scpFrom(sources.getAbsolutePath(), "/" + nvraDataFile.getName() + "/data/" + nvraDataFile.getRemoteName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvraDataFile.getLocalFile());
    }

    // scp  tester@localhost:garbage/nvra/data/name /some/path/rename
    @Test
    public void scpDataFromRelGarbageToDirFile() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data3f1", "dataFile");
        nvraDataFile.createRemote();
        File rename = new File(sources.getAbsolutePath(), "renamed");
        checkFileNotExists(rename);
        int r2 = scpFrom(rename.getAbsolutePath(), "some/garbage/" + nvraDataFile.getName() + "/data/" + nvraDataFile.getRemoteName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(rename);
    }

    // scp  tester@localhost:/another/garbagenvra/data/name /some/path/rename
    @Test
    public void scpDataFromAbsGarbageToDirFile() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvraDataFile = new NvraDataPathsHelper("data4f1", "dataFile");
        nvraDataFile.createRemote();
        File rename = new File(sources.getAbsolutePath(), "renamed");
        checkFileNotExists(rename);
        int r2 = scpFrom(rename.getAbsolutePath(), "/some/garbage/" + nvraDataFile.getName() + "/data/" + nvraDataFile.getRemoteName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(rename);
    }

    @Test
    /*
     * scp  some/path/file1 some/path/file2 tester@localhost:nvra/data
     */
    public void scpMultipleDataFilesTo() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvra1 = new NvraDataPathsHelper("d1tt1", "f1");
        NvraDataPathsHelper nvra2 = new NvraDataPathsHelper("d1tt1", "f2");
        nvra1.createLocal();
        nvra2.createLocal();
        int r = scpTo(nvra1.getName() + "/data", nvra1.getLocalFile().getAbsolutePath(), nvra2.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra1.getRemoteFile());
        checkFileExists(nvra2.getRemoteFile());
    }

    @Test
    /*
     * scp  tester@localhost:nvra1/data/f1 tester@localhost:nvra2/data/f2 dir
     */
    public void scpMultipleDataFilesFrom() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvra1 = new NvraDataPathsHelper("d1ff1", "f1");
        NvraDataPathsHelper nvra2 = new NvraDataPathsHelper("d1ff1", "f2");
        nvra1.createRemote();
        nvra2.createRemote();
        int r = scpFrom(sources.getAbsolutePath(), "some/garbage/" + nvra1.getName() + "/data/" + nvra1.getRemoteName(), "/another/garbage/" + nvra2.getName() + "/data/" + nvra2.getRemoteName());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra1.getLocalFile());
        checkFileExists(nvra2.getLocalFile());
    }

    /*
    LOGS
    DATA/LOGS
     */
    /**
     * logs and data/logs should be synonym for fake koj, thus the
     * uploads/downloads are tested in loops
     */
    private static final String[] LOGS = new String[]{"logs", "data/logs"};

    private static String trasnformToLogId(String s) {
        return s.replace("/", "");
    }

    private class NvraLogsPathsHelper extends NvraDataPathsHelper {

        @Override
        protected String contentKey() {
            return "log";
        }

        public NvraLogsPathsHelper(String id, String name) {
            super(id, name);
        }

        public NvraLogsPathsHelper(String id, String localName, String remoteName) {
            super(id, localName, remoteName);
        }

        public NvraLogsPathsHelper(String vid, String rid, String aid, String name) {
            this(vid, rid, aid, name, name);
        }

        public NvraLogsPathsHelper(String vid, String rid, String aid, String localName, String remoteName) {
            super(vid, rid, aid, localName, remoteName);
        }

        @Override
        public String getStub() {
            //arch!
            return super.getNVRstub() + "/data/logs/" + getArch();
        }

    }

    // scp  /some/path/logname tester@localhost:nvra/{logs,data/logs}
    @Test
    public void scpLogToRelativeNvraLogsNoSlash() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "1To", "logFile");
            nvraLogsFile.createLocal();
            int r2 = scpTo(nvraLogsFile.getName() + "/" + log, nvraLogsFile.getLocalFile().getAbsolutePath());
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getRemoteFile());
        }
    }

    // scp  /some/path/logname tester@localhost:nvra/{logs,data/logs}/
    @Test
    public void scpLogToRelativeNvraLogsSlash() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "2To", "logFile");
            nvraLogsFile.createLocal();
            int r2 = scpTo(nvraLogsFile.getName() + "/" + log + "/", nvraLogsFile.getLocalFile().getAbsolutePath());
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getRemoteFile());
        }

    }

    // scp  /some/path/logname tester@localhost:some/garbage/nvra/{logs,data/logs}
    @Test
    public void scpLogToRelGarbageNvraLogsNoSlash() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "3To", "logFile");
            nvraLogsFile.createLocal();
            int r2 = scpTo("some/garbage/" + nvraLogsFile.getName() + "/" + log, nvraLogsFile.getLocalFile().getAbsolutePath());
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getRemoteFile());
        }
    }

    // scp  /some/path/logname tester@localhost:some/garbage/nvra/{logs,data/logs}/
    @Test
    public void scpLogsToRelGarbageNvraLogsSlash() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "4To", "logFile");
            nvraLogsFile.createLocal();
            int r2 = scpTo("some/garbage/" + nvraLogsFile.getName() + "/" + log + "/", nvraLogsFile.getLocalFile().getAbsolutePath());
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getRemoteFile());
        }
    }

    // scp  /some/path/logname tester@localhost:/some/garbage/nvra/{logs,data/logs}
    @Test
    public void scpLogsToAbsGarbageNvraLogsNoSlash() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "5To", "logFile");
            nvraLogsFile.createLocal();
            int r2 = scpTo("/some/garbage/" + nvraLogsFile.getName() + "/" + log, nvraLogsFile.getLocalFile().getAbsolutePath());
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getRemoteFile());
        }
    }

    // scp  /some/path/logname tester@localhost:nvra/{logs,data/logs}/renamedLog
    @Test
    public void scpLogsToRelNvraLogsRenamed() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "6To", "logFile", "newName");
            nvraLogsFile.createLocal();
            int r2 = scpTo(nvraLogsFile.getName() + "/" + log + "/newName", nvraLogsFile.getLocalFile().getAbsolutePath());
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getRemoteFile());
        }
    }

    // scp  /some/path/logname tester@localhost:some/garbage/nvra/{logs,data/logs}/renamedLog
    @Test
    public void scpLogsToRelGarbageNvraLogsRenamed() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "7To", "logFile", "newName");
            nvraLogsFile.createLocal();
            int r2 = scpTo("some/garabge/" + nvraLogsFile.getName() + "/" + log + "/newName", nvraLogsFile.getLocalFile().getAbsolutePath());
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getRemoteFile());
        }
    }

    // scp  /some/path/logname tester@localhost:/some/garbage/nvra/{logs,data/logs}/renamedLog
    @Test
    public void scpLogsToAbsGarbageNvraLogsRenamed() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "8To", "logFile", "newName");
            nvraLogsFile.createLocal();
            int r2 = scpTo("/some/garabge/" + nvraLogsFile.getName() + "/" + log + "/newName", nvraLogsFile.getLocalFile().getAbsolutePath());
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getRemoteFile());
        }
    }

    // scp  /some/path/logname tester@localhost:{logs,data/logs}
    @Test
    public void scpLogsToRelLogsWrong() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "9To", "logFile");
            nvraLogsFile.createLocal();
            int r2 = scpTo(log, nvraLogsFile.getLocalFile().getAbsolutePath());
            Assert.assertFalse(r2 == 0);
            checkFileNotExists(nvraLogsFile.getRemoteFile());
        }
    }

    // scp  /some/path/logname tester@localhost:/
    @Test
    public void scpLogsToNothing() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "10To", "logFile", "newName");
            nvraLogsFile.createLocal();
            int r2 = scpTo("/", nvraLogsFile.getLocalFile().getAbsolutePath());
            Assert.assertFalse(r2 == 0);
            checkFileNotExists(nvraLogsFile.getRemoteFile());
        }
    }

    // scp  tester@localhost:nvra/{logs,data/logs}/name /some/path/
    @Test
    public void scpLogsFromRelToDir() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "1f1", "logFile");
            nvraLogsFile.createRemote();
            int r2 = scpFrom(sources.getAbsolutePath(), nvraLogsFile.getName() + "/" + log + "/" + nvraLogsFile.getRemoteName());
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getLocalFile());
        }
    }

    // scp  tester@localhost:/nvra/{logs,data/logs}/name /some/path/
    @Test
    public void scpLogsFromAbsToDir() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "2f1", "logFile");
            nvraLogsFile.createRemote();
            int r2 = scpFrom(sources.getAbsolutePath(), "/" + nvraLogsFile.getName() + "/" + log + "/" + nvraLogsFile.getRemoteName());
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getLocalFile());
        }
    }

    // scp  tester@localhost:garbage/nvra/{logs,data/logs}/name /some/path/rename
    @Test
    public void scpLogsFromRelGarbageToDirFile() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "3f1", "logFile");
            nvraLogsFile.createRemote();
            File rename = new File(sources.getAbsolutePath(), "renamed");
            rename.delete();
            checkFileNotExists(rename);
            int r2 = scpFrom(rename.getAbsolutePath(), "some/garbage/" + nvraLogsFile.getName() + "/" + log + "/" + nvraLogsFile.getRemoteName());
            Assert.assertTrue(r2 == 0);
            checkFileExists(rename);
        }
    }

    // scp  tester@localhost:/another/garbagenvra/{logs,data/logs}/name /some/path/rename
    @Test
    public void scpLogsFromAbsGarbageToDirFile() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "4f1", "logFile");
            nvraLogsFile.createRemote();
            File rename = new File(sources.getAbsolutePath(), "renamed");
            rename.delete();
            checkFileNotExists(rename);
            int r2 = scpFrom(rename.getAbsolutePath(), "/some/garbage/" + nvraLogsFile.getName() + "/" + log + "/" + nvraLogsFile.getRemoteName());
            Assert.assertTrue(r2 == 0);
            checkFileExists(rename);
        }
    }

    @Test
    /*
     * scp  some/path/file1 some/path/file2 tester@localhost:nvra/{logs,data/logs}
     */
    public void scpMultipleLogsFilesTo() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvra1 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tt1", "f1");
            NvraLogsPathsHelper nvra2 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tt1", "f2");
            nvra1.createLocal();
            nvra2.createLocal();
            int r = scpTo(nvra1.getName() + "/" + log, nvra1.getLocalFile().getAbsolutePath(), nvra2.getLocalFile().getAbsolutePath());
            Assert.assertTrue(r == 0);
            checkFileExists(nvra1.getRemoteFile());
            checkFileExists(nvra2.getRemoteFile());
        }
    }

    @Test
    /*
     * scp  tester@localhost:nvra1/{logs,data/logs}/f1 tester@localhost:nvra2/{logs,data/logs}/f2 dir
     */
    public void scpMultipleLogsFilesFrom() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvra1 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1ff1", "f1");
            NvraLogsPathsHelper nvra2 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1ff1", "f2");
            nvra1.createRemote();
            nvra2.createRemote();
            int r = scpFrom(sources.getAbsolutePath(), "some/garbage/" + nvra1.getName() + "/" + log + "/" + nvra1.getRemoteName(), "/another/garbage/" + nvra2.getName() + "/" + log + "/" + nvra2.getRemoteName());
            Assert.assertTrue(r == 0);
            checkFileExists(nvra1.getLocalFile());
            checkFileExists(nvra2.getLocalFile());
        }
    }

    //break
    @Test
    public void scpOfDataToIsArchIndependent() throws IOException, InterruptedException {
        NvraDataPathsHelper data2 = new NvraDataPathsHelper("data1tr3", "dataFile2");
        NvraDataPathsHelper data3 = new NvraDataPathsHelper("data1tr3", "data1tr3", "dataX1XtrX3X", "dataFile3");
        NvraDataPathsHelper data4 = new NvraDataPathsHelper("data1tr3", "data1tr3", "dataY1YtrY3Y", "dataFile3");
        data2.createLocal();
        data3.createLocal();
        data4.createSecondaryLocal();
        int r1 = scpTo(data2.getName() + "/data", data2.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r1 == 0);
        int r2 = scpTo(data3.getName() + "/data", data3.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        int r3 = scpTo(data4.getName() + "/data", data4.getSecondaryLocalFile().getAbsolutePath());
        Assert.assertFalse(r3 == 0);
        checkFileExists(data2.getRemoteFile());
        checkFileExists(data3.getRemoteFile());
        checkFileExists(data4.getRemoteFile());
        Assert.assertEquals(data3.getRemoteFile(), data4.getRemoteFile());
    }

    @Test
    public void scpOfLogToIsNotArchIndependent() throws IOException, InterruptedException {
        NvraLogsPathsHelper log2 = new NvraLogsPathsHelper("data1tr3", "logFile2");
        NvraLogsPathsHelper log3 = new NvraLogsPathsHelper("data1tr3", "data1tr3", "dataX1XtrX3X", "logFile3");
        NvraLogsPathsHelper log4 = new NvraLogsPathsHelper("data1tr3", "data1tr3", "dataY1YtrY3Y", "logFile3");
        log2.createLocal();
        log3.createLocal();
        log4.createSecondaryLocal();
        int r1 = scpTo(log2.getName() + "/logs", log2.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r1 == 0);
        int r2 = scpTo(log3.getName() + "/logs", log3.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        int r3 = scpTo(log4.getName() + "/logs", log4.getSecondaryLocalFile().getAbsolutePath());
        Assert.assertTrue(r3 == 0);
        checkFileExists(log2.getRemoteFile());
        checkFileExists(log3.getRemoteFile());
        checkFileExists(log4.getRemoteFile());
        Assert.assertNotEquals(log3.getRemoteFile(), log4.getRemoteFile());
    }

    @Test
    public void scpOfNvraToIsNotArchIndependent() throws IOException, InterruptedException {
        NvraTarballPathsHelper log2 = new NvraTarballPathsHelper("data1tr3");
        NvraTarballPathsHelper log3 = new NvraTarballPathsHelper("data1tr3", "data1tr3", "dataX1XtrX3X");
        NvraTarballPathsHelper log4 = new NvraTarballPathsHelper("data1tr3", "data1tr3", "dataY1YtrY3Y");
        log2.createLocal();
        log3.createLocal();
        log4.createSecondaryLocal();
        int r1 = scpTo(log2.getName(), log2.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r1 == 0);
        int r2 = scpTo(log3.getName(), log3.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        int r3 = scpTo(log4.getName(), log4.getSecondaryLocalFile().getAbsolutePath());
        Assert.assertTrue(r3 == 0);
        checkFileExists(log2.getRemoteFile());
        checkFileExists(log3.getRemoteFile());
        checkFileExists(log4.getRemoteFile());
        Assert.assertNotEquals(log3.getRemoteFile(), log4.getRemoteFile());
    }

    /*
    generic/path
    ****************
    support of those is discutable
     */
    private class NvraGeneralPathsHelper extends NvraDataPathsHelper {

        private final String remotePath;

        @Override
        protected String contentKey() {
            return "generic";
        }

        public NvraGeneralPathsHelper(String id, String remotePath, String name) {
            super(id, name);
            this.remotePath = remotePath;
        }

        public NvraGeneralPathsHelper(String id, String remotePath, String localName, String remoteName) {
            super(id, localName, remoteName);
            this.remotePath = remotePath;
        }

        @Override
        public String getStub() {
            //arch!
            return super.getNVRstub() + "/" + getArch() + "/" + remotePath;
        }

        public String getRemotePath() {
            return remotePath;
        }

        public String getRemoteTail() {
            return getName() + "/" + remotePath;
        }

    }

    // scp  /another/path/name1 tester@localhost:nvra/some/path/name2
    @Test
    public void scpGenericPathToRename() throws IOException, InterruptedException {
        title(2);
        NvraGeneralPathsHelper genericFile = new NvraGeneralPathsHelper("generic1t1", "some/path", "fileName1", "fileName2");
        genericFile.createLocal();
        int r2 = scpTo(genericFile.getRemoteTail() + "/" + genericFile.getRemoteName(), genericFile.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(genericFile.getRemoteFile());
    }

    // scp  /another/path/name1 tester@localhost:nvra/some/path/
    @Test
    public void scpGenericPathTo() throws IOException, InterruptedException {
        title(2);
        NvraGeneralPathsHelper genericFile = new NvraGeneralPathsHelper("generic1t2", "some/path", "fileName1");
        genericFile.createLocal();
        int r2 = scpTo(genericFile.getRemoteTail(), genericFile.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        //note, here koji evaluated it as rename!
        checkFileNotExists(genericFile.getRemoteFile());
        checkFileExists(genericFile.getRemoteFile().getParentFile());
    }

    // scp  tester@localhost:nvra/some/path/name2  /another/path/name1
    @Test
    public void scpGenericPathFromWithRename() throws IOException, InterruptedException {
        title(2);
        NvraGeneralPathsHelper genericFile = new NvraGeneralPathsHelper("generic1f1", "some/path", "fileName1", "fileName2");
        genericFile.createRemote();
        int r2 = scpFrom(genericFile.getLocalFile().getAbsolutePath(), genericFile.getRemoteTail() + "/" + genericFile.getRemoteName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(genericFile.getLocalFile());
    }

    // scp  tester@localhost:nvra/some/path/name1 /another/path/
    @Test
    public void scpGenericPathFrom() throws IOException, InterruptedException {
        title(2);
        NvraGeneralPathsHelper genericFile = new NvraGeneralPathsHelper("generic1f2", "some/path", "fileName1");
        genericFile.createRemote();
        int r2 = scpFrom(genericFile.getLocalFile().getParentFile().getAbsolutePath(), genericFile.getRemoteTail() + "/" + genericFile.getRemoteName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(genericFile.getLocalFile());
    }

    /*
     -r uploads
     -r uploads should be provided  on its own
     */
 /*
     binaries
     */
    @Test
    //scp -r /abs/path/(nvra1) tester@localhost:
    public void scpNvraFromDir() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("dr1t1");
        nvra.createLocal();
        int r1 = scpTo("", nvra.getLocalFile().getParent());
        Assert.assertFalse(r1 == 0);
        checkFileNotExists(nvra.getRemoteFile());

        int r2 = scpTo(RECURSIVE, "", null, nvra.getLocalFile().getParent());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    //scp  -r /abs/path/ (nvra1) tester@localhost:nvra1
    public void scpNvraFromDirToSameNvra() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra1 = new NvraTarballPathsHelper("dr1t2");
        nvra1.createLocal();
        int r = scpTo(RECURSIVE, nvra1.getName(), null, nvra1.getLocalFile().getParent());
        Assert.assertTrue(r == 0);
        checkFileExists(nvra1.getRemoteFile());
    }

    @Test
    //scp -r /abs/path/(nvra1)(nvra11) tester@localhost:nvra2
    public void scpNvraFromDirToAnotherNvra() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra1 = new NvraTarballPathsHelper("dr1t3");
        NvraTarballPathsHelper nvra11 = new NvraTarballPathsHelper("dr11t33");
        NvraTarballPathsHelper nvra2 = new NvraTarballPathsHelper("dr2t2");
        nvra1.createLocal();
        nvra11.createLocal();
        int r = scpTo(RECURSIVE, nvra2.getName(), null, nvra1.getLocalFile().getParent());
        Assert.assertFalse(r == 0);
        //must fail now, as it is copying several files to single one, and overwrite is disabled now
        //however, the file must be created
        checkFileExists(nvra2.getRemoteFile());
        //TODO fix this?
        //not unless more binaries per  NVRA are allowed
        //however, to allow them is advised, to allow rpm-like "subpakcages"
    }

    @Test
    //scp -r /abs/path/(nvra1,nvra2) tester@localhost:
    public void scpMoreNvraFromDir() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra1 = new NvraTarballPathsHelper("dr4t1");
        NvraTarballPathsHelper nvra2 = new NvraTarballPathsHelper("dr4t2");
        nvra1.createLocal();
        nvra2.createLocal();
        int r1 = scpTo(RECURSIVE, "", null, sources.getAbsolutePath());
        Assert.assertTrue(r1 == 0);
        checkFileExists(nvra1.getRemoteFile());
        checkFileExists(nvra2.getRemoteFile());
    }

    /*
     binaries with subdirs
     */
    @Test
    //scp -r /abs/path/(subath/nvra1) tester@localhost:
    public void scpNvraFromDirWithSubdir() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("dr3t1");
        nvra.createLocal();
        File subdir = new File(sources, "subdir");
        File subdirFile = new File(subdir, nvra.getName());
        subdir.mkdir();
        nvra.getLocalFile().renameTo(subdirFile);
        int r2 = scpTo(RECURSIVE, "", null, sources.getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    //scp -r /abs/path/(nvra2likePath/nvra1) tester@localhost:nvra3
    public void scpNvraFromDirWithNvraLIkeSubdirToNvra() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra1 = new NvraTarballPathsHelper("dr3t3");
        NvraTarballPathsHelper nvra11 = new NvraTarballPathsHelper("dr33t33");
        NvraTarballPathsHelper nvra2 = new NvraTarballPathsHelper("dr3t2");
        nvra1.createLocal();
        File subdir = new File(sources, nvra11.getName());
        File subdirFile = new File(subdir, nvra1.getName());
        subdir.mkdir();
        nvra1.getLocalFile().renameTo(subdirFile);
        int r2 = scpTo(RECURSIVE, nvra2.getName(), null, sources.getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        //not surprisingly, the target name is used
        checkFileExists(nvra2.getRemoteFile());
        checkFileNotExists(nvra11.getRemoteFile());
        checkFileNotExists(nvra1.getRemoteFile());
    }

    @Test
    //scp -r /abs/path/(nvra2likePath/nvra1) tester@localhost:
    public void scpNvraFromDirWithNvraLIkeSubdir() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra1 = new NvraTarballPathsHelper("dr3t4");
        NvraTarballPathsHelper nvra11 = new NvraTarballPathsHelper("dr33t44");
        nvra1.createLocal();
        File subdir = new File(sources, nvra11.getName());
        File subdirFile = new File(subdir, nvra1.getName());
        subdir.mkdir();
        nvra1.getLocalFile().renameTo(subdirFile);
        int r2 = scpTo(RECURSIVE, "", null, sources.getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        //correctly, the name of file, not dir s used
        checkFileExists(nvra1.getRemoteFile());
        checkFileNotExists(nvra11.getRemoteFile());
    }

    @Test
    //scp -r /abs/path/nvra2/(nvra1) tester@localhost:
    public void scpNvraFromNvraLikeDir() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra1 = new NvraTarballPathsHelper("dr3t5");
        NvraTarballPathsHelper nvra11 = new NvraTarballPathsHelper("dr33t55");
        nvra1.createLocal();
        File subdir = new File(sources, nvra11.getName());
        File subdirFile = new File(subdir, nvra1.getName());
        subdir.mkdir();
        nvra1.getLocalFile().renameTo(subdirFile);
        int r2 = scpTo(RECURSIVE, "", null, subdir.getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        //correctly, the name of file, not dir s used
        checkFileExists(nvra1.getRemoteFile());
        checkFileNotExists(nvra11.getRemoteFile());
    }

    /*
     data
     */
    @Test
    //scp -r /abs/path/file tester@localhost:nvra/data
    public void scpDirWithDataFileToNvraData() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvra = new NvraDataPathsHelper("data1tr1", "dataFile1");
        nvra.createLocal();
        int r1 = scpTo(RECURSIVE, "", null, nvra.getLocalFile().getParent());
        Assert.assertFalse(r1 == 0);
        int r2 = scpTo(RECURSIVE, nvra.getName() + "/data", null, nvra.getLocalFile().getParent());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    //scp -r /abs/path/data/file tester@localhost:{,nvra}
    public void scpDataDirWithDataFileToNvra() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvra = new NvraDataPathsHelper("data1tr2", "dataFile2");
        nvra.createLocal();
        File subdir = new File(sources, "data");
        File subfile = new File(subdir, nvra.getLocalName());
        subdir.mkdir();
        nvra.getLocalFile().renameTo(subfile);
        int r1 = scpTo(RECURSIVE, nvra.getName() + "/", null, subdir.getAbsolutePath());
        //data in path are ignored, and so the datafile ends as NVRA instead of DATA:(
        Assert.assertTrue(r1 == 0);
        checkFileNotExists(nvra.getRemoteFile());
        int r2 = scpTo(RECURSIVE, nvra.getName() + "/data/", null, subdir.getAbsolutePath());
        //this oone uploads correctly
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getRemoteFile());
    }

    @Test
    //scp -r /abs/path/data/file tester@localhost:{,nvra}
    public void scpMultipleDataFilesInMultipleDirsToNvra() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper nvra1 = new NvraDataPathsHelper("data1tr3", "dataFile3");
        NvraDataPathsHelper nvra2 = new NvraDataPathsHelper("data1tr3", "dataFile4");
        nvra1.createLocal();
        nvra2.createLocal();
        File subdir = new File(sources, "log"); //being confusive?-)
        File subfile = new File(subdir, nvra1.getLocalName());
        subdir.mkdir();
        nvra1.getLocalFile().renameTo(subfile);
        int r2 = scpTo(RECURSIVE, nvra1.getName() + "/data/", null, sources.getAbsolutePath());
        //this oone uploads correctly
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra1.getRemoteFile());
        checkFileExists(nvra2.getRemoteFile());
    }

    /*
     logs
     */
    @Test
    //scp -r /abs/path/file tester@localhost:nvra/data
    public void scpDirWithLogFileToNvraLog() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr1", "logFile1");
            nvraLogsFile.createLocal();
            int r1 = scpTo(RECURSIVE, "", null, nvraLogsFile.getLocalFile().getParent());
            Assert.assertFalse(r1 == 0);
            int r2 = scpTo(RECURSIVE, nvraLogsFile.getName() + "/" + log, null, nvraLogsFile.getLocalFile().getParent());
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getRemoteFile());
        }
    }

    @Test
    //scp -r /abs/path/data/file tester@localhost:{,nvra}
    public void scpLogDirWithLogFileToNvra() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr2", "logFile2");
            nvraLogsFile.createLocal();
            File subdir = new File(sources, "logs");
            File subfile = new File(subdir, nvraLogsFile.getLocalName());
            subdir.mkdir();
            nvraLogsFile.getLocalFile().renameTo(subfile);
            int r1 = scpTo(RECURSIVE, nvraLogsFile.getName() + "/", null, subdir.getAbsolutePath());
            //data in path are ignored, and so the datafile ends as NVRA instead of DATA:(
            Assert.assertTrue(r1 == 0);
            checkFileNotExists(nvraLogsFile.getRemoteFile());
            int r2 = scpTo(RECURSIVE, nvraLogsFile.getName() + "/" + log + "/", null, subdir.getAbsolutePath());
            //this oone uploads correctly
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile.getRemoteFile());
        }
    }

    @Test
    //scp -r /abs/path/data/file tester@localhost:{,nvra}
    public void scpMultipleLogFilesInMultipleDirsToNvra() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            NvraLogsPathsHelper nvraLogsFile1 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr3", "logFile3");
            NvraLogsPathsHelper nvraLogsFile2 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr3", "logFile4");
            nvraLogsFile1.createLocal();
            nvraLogsFile2.createLocal();
            File subdir = new File(sources, "data"); //being confusive?-)
            File subfile = new File(subdir, nvraLogsFile1.getLocalName());
            subdir.mkdir();
            nvraLogsFile1.getLocalFile().renameTo(subfile);
            int r2 = scpTo(RECURSIVE, nvraLogsFile1.getName() + "/" + log + "/", null, sources.getAbsolutePath());
            //this oone uploads correctly
            Assert.assertTrue(r2 == 0);
            checkFileExists(nvraLogsFile1.getRemoteFile());
            checkFileExists(nvraLogsFile2.getRemoteFile());
        }
    }

    /*
     custom paths
     */
    @Test
    //scp -r /abs/path/file tester@localhost:nvra/data
    public void scpDirWithFilesFileToNvraPath() throws IOException, InterruptedException {
        title(2);
        NvraGeneralPathsHelper nvra = new NvraGeneralPathsHelper("data1tr1", "some/path", "dataFile1");
        nvra.createLocal();
        int r1 = scpTo(RECURSIVE, "", null, nvra.getLocalFile().getParent());
        Assert.assertFalse(r1 == 0);
        int r2 = scpTo(RECURSIVE, nvra.getName() + "/some/path", null, nvra.getLocalFile().getParent());
        Assert.assertTrue(r2 == 0);
        //!!broken!
        //checkFileExists(nvra.getRemoteFile());
        //this is wrong, there hsoudl be some/path/dataFile1, but is just some/path (file of path)
        checkFileExists(nvra.getRemoteFile().getParentFile());
    }

    @Test
    //scp -r /abs/path/data/file tester@localhost:{,nvra}
    public void scpMultipleFilesInMultipleDirsToNvraPath() throws IOException, InterruptedException {
        title(2);
        NvraGeneralPathsHelper nvra1 = new NvraGeneralPathsHelper("data1tr3", "some/path", "dataFile3");
        NvraGeneralPathsHelper nvra2 = new NvraGeneralPathsHelper("data1tr3", "some/path", "dataFile4");
        nvra1.createLocal();
        nvra2.createLocal();
        File subdir = new File(sources, "log"); //being confusive?-)
        File subfile = new File(subdir, nvra1.getLocalName());
        subdir.mkdir();
        nvra1.getLocalFile().renameTo(subfile);
        int r2 = scpTo(RECURSIVE, nvra1.getName() + "/some/path/", null, sources.getAbsolutePath());
        //broken! first file, although different is trying to overwrite second file
        //Assert.assertTrue(r2 == 0);
        Assert.assertFalse(r2 == 0);
        //checkFileExists(nvra1.getRemoteFile());
        //checkFileExists(nvra2.getRemoteFile());
        checkFileExists(nvra1.getRemoteFile().getParentFile());
    }

    /**
     * ***********************************************************************
     * ***************Recursive downloads*************************************
     * ***********************************************************************
     */
    @Test
    //scp -r tester@localhost:nvra /abs/path/
    //where  tester@localhost:nvra  contains one, correct file
    public void scpRecursciveNvraFrom() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("r1f1");
        nvra.createRemote();
        int r2 = scpFrom(RECURSIVE, nvra.getLocalFile().getParent(), null, nvra.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getLocalFile());
    }

    @Test
    //scp -r tester@localhost:nvra /abs/path/
    //where  tester@localhost:nvra  contains two, correct files
    public void scpRecursciveMultipleNvraFrom() throws IOException, InterruptedException {
        title(2);
        NvraTarballPathsHelper nvra = new NvraTarballPathsHelper("r1f1");
        File strangeFileRemote = new File(nvra.getRemoteFile().getParent(), nvra.getName() + "xxx");
        nvra.createRemote();
        strangeFileRemote.createNewFile();
        checkFileExists(strangeFileRemote);
        int r2 = scpFrom(RECURSIVE, nvra.getLocalFile().getParent(), null, nvra.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(nvra.getLocalFile());
        File strangeFileLocal = new File(nvra.getLocalFile().getParent(), strangeFileRemote.getName());
        //BROKEN! scp from NVRA simply ignores -r and returns the file it got in src
        //todo fixit?
        checkFileNotExists(strangeFileLocal);
    }

    //data
    @Test
    //scp -r tester@localhost:nvra/data /abs/path/
    //where  tester@localhost:nvra/data  can contain really a lot!
    public void scpRecursciveMultipleDataFrom() throws IOException, InterruptedException {
        title(2);
        NvraDataPathsHelper data1 = new NvraDataPathsHelper("data1tr3", "dataFile1");
        NvraDataPathsHelper data2 = new NvraDataPathsHelper("data1tr3", "dataFile2");
        NvraDataPathsHelper data3 = new NvraDataPathsHelper("data1tr3", "data1tr3", "dataX1XtrX3X", "dataFile3");
        //NvraDataPathsHelper data4 = new NvraDataPathsHelper("data1tr3", "data1tr3", "dataY1YtrY3Y", "dataFile3");
        NvraLogsPathsHelper log1 = new NvraLogsPathsHelper("data1tr3", "logFile1");
        NvraLogsPathsHelper log2 = new NvraLogsPathsHelper("data1tr3", "logFile2");
        NvraLogsPathsHelper log3 = new NvraLogsPathsHelper("data1tr3", "data1tr3", "dataX1XtrX3X", "logFile3");
        NvraLogsPathsHelper log4 = new NvraLogsPathsHelper("data1tr3", "data1tr3", "dataY1YtrY3Y", "logFile3");
        data1.createRemote();
        data2.createRemote();
        data3.createRemote();
        log1.createRemote();
        log2.createRemote();
        log3.createRemote();
        log4.createRemote();
        int r2 = scpFrom(RECURSIVE, sources.getAbsolutePath(), null, data1.getName() + "/data");
        Assert.assertFalse(r2 == 0);
        //BROKEN! but should work once this is updated ro final sshd-core version
        //fixme!
    }

    //logs
    @Test
    //scp -r tester@localhost:nvra/logs /abs/path/
    //where  tester@localhost:nvra/logs  contans everal logs of given arch
    public void scpRecursciveMultipleLogsFrom1() throws IOException, InterruptedException {
        title(2);
        {
            String log = LOGS[0];
            NvraLogsPathsHelper log1 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr4", "logFile1");
            NvraLogsPathsHelper log2 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr4", "logFile2");
            NvraLogsPathsHelper log3 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr4", trasnformToLogId(log) + "1tr4", trasnformToLogId(log) + "X1XtrX4X", "logFile3");
            NvraLogsPathsHelper log4 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr4", trasnformToLogId(log) + "1tr4", trasnformToLogId(log) + "Y1YtrY4Y", "logFile3");
            log1.createRemote();
            log2.createRemote();
            log3.createRemote();
            log4.createRemote();
            int r1 = scpFrom(RECURSIVE, sources.getAbsolutePath(), null, log1.getName() + "/" + log);
            Assert.assertTrue(r1 == 0);
            int r2 = scpFrom(RECURSIVE, sources.getAbsolutePath(), null, log3.getName() + "/" + log);
            Assert.assertTrue(r2 == 0);
            int r3 = scpFrom(RECURSIVE, sources.getAbsolutePath(), null, log4.getName() + "/" + log);
            Assert.assertTrue(r3 == 0);
            //scp honors the directories
            checkFileExists(new File(new File(log1.getLocalFile().getParent(), log1.getArch()), log1.getLocalName()));
            checkFileExists(new File(new File(log2.getLocalFile().getParent(), log2.getArch()), log2.getLocalName()));
            checkFileExists(new File(new File(log3.getLocalFile().getParent(), log3.getArch()), log3.getLocalName()));
            checkFileExists(new File(new File(log4.getLocalFile().getParent(), log4.getArch()), log4.getLocalName()));
        }
    }

    @Test
    //scp -r tester@localhost:nvra/data/logs /abs/path/
    //where  tester@localhost:nvra/datalogs  contans everal logs of given arch
    public void scpRecursciveMultipleLogsFrom2() throws IOException, InterruptedException {
        title(2);
        {
            String log = LOGS[1];
            NvraLogsPathsHelper log1 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr4", "logFile1");
            NvraLogsPathsHelper log2 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr4", "logFile2");
            NvraLogsPathsHelper log3 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr4", trasnformToLogId(log) + "1tr4", trasnformToLogId(log) + "X1XtrX4X", "logFile3");
            NvraLogsPathsHelper log4 = new NvraLogsPathsHelper(trasnformToLogId(log) + "1tr4", trasnformToLogId(log) + "1tr4", trasnformToLogId(log) + "Y1YtrY4Y", "logFile3");
            log1.createRemote();
            log2.createRemote();
            log3.createRemote();
            log4.createRemote();
            int r1 = scpFrom(RECURSIVE, sources.getAbsolutePath(), null, log1.getName() + "/" + log);
            Assert.assertFalse(r1 == 0);
            int r2 = scpFrom(RECURSIVE, sources.getAbsolutePath(), null, log3.getName() + "/" + log);
            Assert.assertFalse(r2 == 0);
            int r3 = scpFrom(RECURSIVE, sources.getAbsolutePath(), null, log4.getName() + "/" + log);
            Assert.assertFalse(r3 == 0);
            //for some reason
            //logs works (see scpRecursciveMultipleLogsFrom1) but data/logs not
            //fixme!
            //once fixed, those tests should be loope din same way as oter logs tests
        }
    }
    //custom/path
    //not bothering withthem
    //
    //multiple NVRA-like files into single NVRA
    //curently not supported

    /**
     * *************************************************************************
     * ****************Special cases for upload of NVRA***********************
     * *************************************************************************
     */
    /*
    uplaod of failed
     */
    //this works because of renaming and  custom/paths implementation
    // scp  FAILED tester@localhost:nvra/FAILED
    @Test
    public void scpWrongNameToNvra() throws IOException, InterruptedException {
        title(2);
        NvraGeneralPathsHelper genericFile = new NvraGeneralPathsHelper("failedFileT1", "", "FAILED");
        genericFile.createLocal();
        int r2 = scpTo(genericFile.getRemoteTail() + "/" + genericFile.getRemoteName(), genericFile.getLocalFile().getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(genericFile.getRemoteFile());
    }

    @Test
    public void scpWrongNameFromNvra() throws IOException, InterruptedException {
        title(2);
        NvraGeneralPathsHelper genericFile = new NvraGeneralPathsHelper("failedFileF1", "", "FAILED");
        genericFile.createRemote();
        int r1 = scpFrom(sources.getAbsolutePath(), genericFile.getRemoteTail());
        Assert.assertFalse(r1 == 0);
        checkFileNotExists(genericFile.getLocalFile());
        int r2 = scpFrom(sources.getAbsolutePath(), genericFile.getRemoteTail() + "/" + genericFile.getRemoteName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(genericFile.getLocalFile());
    }

    //scp of java-1.8.0-openjdk-jdk8u172.b00-44.static.x86_64.tarxz
    //and of java-1.8.0-openjdk-jdk8u172.b00-44.upstream.src.tarxz
    //must get same target parent od 
    //java-1.8.0-openjdk/jdk8u172.b00/44.upstream/
    //with different on arch only
    //java-1.8.0-openjdk/jdk8u172.b00/44.upstream/x86_64/
    //java-1.8.0-openjdk/jdk8u172.b00/44.upstream/src/
    //this behavior is hardcoded and shoudl be cursed
    @Test
    public void testSingleStaticGotMappedToUsptramTo() throws IOException, InterruptedException {
        title(2);
        File sharedTopDir = new File(kojiDb.getAbsolutePath() + "/java-1.8.0-openjdk/jdk8u172.b00/44.upstream/");
        File x64Dir = new File(sharedTopDir, "x86_64");
        File srcDir = new File(sharedTopDir, "src");
        File x64File = new File(x64Dir, "java-1.8.0-openjdk-jdk8u172.b00-44.static.x86_64.tarxz");
        File srcFile = new File(srcDir, "java-1.8.0-openjdk-jdk8u172.b00-44.upstream.src.tarxz");
        File x64FileLocal = new File(sources, x64File.getName());
        File srcFileLocal = new File(sources, srcFile.getName());
        createFile(x64FileLocal, x64FileLocal.getName());
        checkFileExists(x64FileLocal);
        createFile(srcFileLocal, srcFileLocal.getName());
        checkFileExists(srcFileLocal);
        int r1 = scpTo("", srcFileLocal.getAbsolutePath());
        Assert.assertTrue(r1 == 0);
        int r2 = scpTo("", x64FileLocal.getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(srcFile);
        checkFileExists(x64File);

    }

    @Test
    public void testSingleStaticGotMappedToUsptramFrom() throws IOException, InterruptedException {
        title(2);
        File sharedTopDir = new File(kojiDb.getAbsolutePath() + "/java-1.8.0-openjdk/jdk8u172.b00/44.upstream/");
        File x64Dir = new File(sharedTopDir, "x86_64");
        File srcDir = new File(sharedTopDir, "src");
        File x64File = new File(x64Dir, "java-1.8.0-openjdk-jdk8u172.b00-44.static.x86_64.tarxz");
        File srcFile = new File(srcDir, "java-1.8.0-openjdk-jdk8u172.b00-44.upstream.src.tarxz");
        File x64FileLocal = new File(sources, x64File.getName());
        File srcFileLocal = new File(sources, srcFile.getName());
        x64Dir.mkdirs();
        createFile(x64File, x64File.getName());
        checkFileExists(x64File);
        srcDir.mkdirs();
        createFile(srcFile, srcFile.getName());
        checkFileExists(srcFile);
        int r1 = scpFrom(sources.getAbsolutePath(), srcFile.getName());
        Assert.assertTrue(r1 == 0);
        int r2 = scpFrom(sources.getAbsolutePath(), x64File.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(srcFileLocal);
        checkFileExists(x64FileLocal);

    }

    @Test
    public void testMultipleStaticGotMappedToUsptramTo() throws IOException, InterruptedException {
        title(2);
        File sharedTopDir = new File(kojiDb.getAbsolutePath() + "/java-1.8.0-openjdk/jdk8u172.b00/44.upstream/");
        File x64Dir = new File(sharedTopDir, "x86_64");
        File srcDir = new File(sharedTopDir, "src");
        File x64File = new File(x64Dir, "java-1.8.0-openjdk-jdk8u172.b00-44.static.x86_64.tarxz");
        File srcFile = new File(srcDir, "java-1.8.0-openjdk-jdk8u172.b00-44.upstream.src.tarxz");
        File x64FileLocal = new File(sources, x64File.getName());
        File srcFileLocal = new File(sources, srcFile.getName());
        createFile(x64FileLocal, x64FileLocal.getName());
        checkFileExists(x64FileLocal);
        createFile(srcFileLocal, srcFileLocal.getName());
        checkFileExists(srcFileLocal);
        int r1 = scpTo("", srcFileLocal.getAbsolutePath(), x64FileLocal.getAbsolutePath());
        Assert.assertTrue(r1 == 0);
        checkFileExists(srcFile);
        checkFileExists(x64File);

    }

    @Test
    public void testMultipleStaticGotMappedToUsptramFrom2() throws IOException, InterruptedException {
        title(2);
        File sharedTopDir = new File(kojiDb.getAbsolutePath() + "/java-1.8.0-openjdk/jdk8u172.b00/44.upstream/");
        File x64Dir = new File(sharedTopDir, "x86_64");
        File srcDir = new File(sharedTopDir, "src");
        File x64File = new File(x64Dir, "java-1.8.0-openjdk-jdk8u172.b00-44.static.x86_64.tarxz");
        File srcFile = new File(srcDir, "java-1.8.0-openjdk-jdk8u172.b00-44.upstream.src.tarxz");
        File x64FileLocal = new File(sources, x64File.getName());
        File srcFileLocal = new File(sources, srcFile.getName());
        x64Dir.mkdirs();
        createFile(x64File, x64File.getName());
        checkFileExists(x64File);
        srcDir.mkdirs();
        createFile(srcFile, srcFile.getName());
        checkFileExists(srcFile);
        int r2 = scpFrom(sources.getAbsolutePath(), srcFile.getName(), x64File.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(srcFileLocal);
        checkFileExists(x64FileLocal);

    }

    @Test
    public void testSingleStaticGotMappedToUsptramToRenamed() throws IOException, InterruptedException {
        title(2);
        File sharedTopDir = new File(kojiDb.getAbsolutePath() + "/java-1.8.0-openjdk/jdk8u172.b00/44.upstream/");
        File x64Dir = new File(sharedTopDir, "x86_64");
        File srcDir = new File(sharedTopDir, "src");
        File x64File = new File(x64Dir, "java-1.8.0-openjdk-jdk8u172.b00-44.static.x86_64.tarxz");
        File srcFile = new File(srcDir, "java-1.8.0-openjdk-jdk8u172.b00-44.upstream.src.tarxz");
        File x64FileLocal = new File(sources, x64File.getName());
        File srcFileLocal = new File(sources, srcFile.getName());
        createFile(x64FileLocal, x64FileLocal.getName());
        checkFileExists(x64FileLocal);
        createFile(srcFileLocal, srcFileLocal.getName());
        checkFileExists(srcFileLocal);
        int r1 = scpTo(x64File.getName(), srcFileLocal.getAbsolutePath());
        Assert.assertTrue(r1 == 0);
        checkFileExists(x64File);
        int r2 = scpTo(srcFile.getName(), x64FileLocal.getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(srcFile);

    }

    @Test
    public void testSingleStaticGotMappedToUsptramToLog() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            File sharedTopDir = new File(kojiDb.getAbsolutePath() + "/java-1.8.0-openjdk/jdk8u172." + trasnformToLogId(log) + ".b00/44.upstream/data/logs");
            File x64Dir = new File(sharedTopDir, "x86_64");
            File srcDir = new File(sharedTopDir, "src");
            String x64Nvra = "java-1.8.0-openjdk-jdk8u172." + trasnformToLogId(log) + ".b00-44.static.x86_64.tarxz";
            String srcNvra = "java-1.8.0-openjdk-jdk8u172." + trasnformToLogId(log) + ".b00-44.upstream.src.tarxz";
            File x64LogFile = new File(x64Dir, "log64");
            File srcLogFile = new File(srcDir, "logSrc");
            File x64LogFileLocal = new File(sources, x64LogFile.getName());
            File srcLogFileLocal = new File(sources, srcLogFile.getName());
            createFile(x64LogFileLocal, x64Nvra);
            checkFileExists(x64LogFileLocal);
            createFile(srcLogFileLocal, srcNvra);
            checkFileExists(srcLogFileLocal);
            int r1 = scpTo(srcNvra + "/" + log, srcLogFileLocal.getAbsolutePath());
            Assert.assertTrue(r1 == 0);
            int r2 = scpTo(x64Nvra + "/" + log, x64LogFileLocal.getAbsolutePath());
            Assert.assertTrue(r2 == 0);
            checkFileExists(srcLogFile);
            checkFileExists(x64LogFile);
        }

    }

    @Test
    public void testSingleStaticGotMappedToUsptramToData() throws IOException, InterruptedException {
        title(2);
        File sharedTopDir = new File(kojiDb.getAbsolutePath() + "/java-1.8.0-openjdk/jdk8u172.b00/44.upstream/data/");
        String x64Nvra = "java-1.8.0-openjdk-jdk8u172.b00-44.static.x86_64.tarxz";
        String srcNvra = "java-1.8.0-openjdk-jdk8u172.b00-44.upstream.src.tarxz";
        File x64LogFile = new File(sharedTopDir, "log64");
        File srcLogFile = new File(sharedTopDir, "logSrc");
        File x64LogFileLocal = new File(sources, x64LogFile.getName());
        File srcLogFileLocal = new File(sources, srcLogFile.getName());
        createFile(x64LogFileLocal, x64Nvra);
        checkFileExists(x64LogFileLocal);
        createFile(srcLogFileLocal, srcNvra);
        checkFileExists(srcLogFileLocal);
        int r1 = scpTo(srcNvra + "/data", srcLogFileLocal.getAbsolutePath());
        Assert.assertTrue(r1 == 0);
        int r2 = scpTo(x64Nvra + "/data", x64LogFileLocal.getAbsolutePath());
        Assert.assertTrue(r2 == 0);
        checkFileExists(srcLogFile);
        checkFileExists(x64LogFile);
    }

    @Test
    public void testSingleStaticGotMappedToUsptramFromLog() throws IOException, InterruptedException {
        title(2);
        for (String log : LOGS) {
            File sharedTopDir = new File(kojiDb.getAbsolutePath() + "/java-1.8.0-openjdk/jdk8u172." + trasnformToLogId(log) + ".b00/44.upstream/data/logs");
            File x64Dir = new File(sharedTopDir, "x86_64");
            File srcDir = new File(sharedTopDir, "src");
            String x64Nvra = "java-1.8.0-openjdk-jdk8u172." + trasnformToLogId(log) + ".b00-44.static.x86_64.tarxz";
            String srcNvra = "java-1.8.0-openjdk-jdk8u172." + trasnformToLogId(log) + ".b00-44.upstream.src.tarxz";
            srcDir.mkdirs();
            x64Dir.mkdirs();
            File x64LogFile = new File(x64Dir, "log64");
            File srcLogFile = new File(srcDir, "logSrc");
            File x64LogFileLocal = new File(sources, x64LogFile.getName());
            File srcLogFileLocal = new File(sources, srcLogFile.getName());
            createFile(x64LogFile, x64Nvra);
            checkFileExists(x64LogFile);
            createFile(srcLogFile, srcNvra);
            checkFileExists(srcLogFile);
            int r1 = scpFrom(sources.getAbsolutePath(), x64Nvra + "/" + log + "/" + x64LogFile.getName());
            Assert.assertTrue(r1 == 0);
            int r2 = scpFrom(sources.getAbsolutePath(), srcNvra + "/" + log + "/" + srcLogFile.getName());
            Assert.assertTrue(r2 == 0);
            checkFileExists(srcLogFileLocal);
            checkFileExists(x64LogFileLocal);
        }

    }

    @Test
    public void testSingleStaticGotMappedToUsptramFromData() throws IOException, InterruptedException {
        title(2);
        File sharedTopDir = new File(kojiDb.getAbsolutePath() + "/java-1.8.0-openjdk/jdk8u172.b00/44.upstream/data/");
        String x64Nvra = "java-1.8.0-openjdk-jdk8u172.b00-44.static.x86_64.tarxz";
        String srcNvra = "java-1.8.0-openjdk-jdk8u172.b00-44.upstream.src.tarxz";
        File x64LogFile = new File(sharedTopDir, "log64");
        File srcLogFile = new File(sharedTopDir, "logSrc");
        File x64LogFileLocal = new File(sources, x64LogFile.getName());
        File srcLogFileLocal = new File(sources, srcLogFile.getName());
        sharedTopDir.mkdirs();
        createFile(x64LogFile, x64Nvra);
        checkFileExists(x64LogFile);
        createFile(srcLogFile, srcNvra);
        checkFileExists(srcLogFile);
        int r1 = scpFrom(sources.getAbsolutePath(), srcNvra + "/data/" + srcLogFile.getName());
        Assert.assertTrue(r1 == 0);
        int r2 = scpFrom(sources.getAbsolutePath(), x64Nvra + "/data/" + x64LogFile.getName());
        Assert.assertTrue(r2 == 0);
        checkFileExists(srcLogFileLocal);
        checkFileExists(x64LogFileLocal);
    }

}
