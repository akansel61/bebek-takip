package com.akansel.bebektakip.store;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dışa bağımlılığı olmayan küçük bir JSON okuyucu / yazıcı.
 *
 * Java karşılıkları:
 *   nesne -> LinkedHashMap, dizi -> ArrayList, metin -> String,
 *   sayı  -> Double, doğruluk -> Boolean, boş -> null
 */
public final class Json {

    private Json() {
    }

    /** Nesneyi girintili, okunabilir JSON metnine çevirir. */
    public static String yaz(Object deger) {
        StringBuilder sb = new StringBuilder();
        yazDeger(deger, sb, 0);
        return sb.toString();
    }

    private static void yazDeger(Object d, StringBuilder sb, int seviye) {
        if (d == null) {
            sb.append("null");
        } else if (d instanceof String) {
            yazMetin((String) d, sb);
        } else if (d instanceof Boolean) {
            sb.append(d.toString());
        } else if (d instanceof Number) {
            yazSayi((Number) d, sb);
        } else if (d instanceof Map) {
            yazNesne((Map<?, ?>) d, sb, seviye);
        } else if (d instanceof List) {
            yazDizi((List<?>) d, sb, seviye);
        } else {
            yazMetin(String.valueOf(d), sb);
        }
    }

    private static void yazNesne(Map<?, ?> m, StringBuilder sb, int seviye) {
        if (m.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{\n");
        int i = 0;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            girinti(sb, seviye + 1);
            yazMetin(String.valueOf(e.getKey()), sb);
            sb.append(": ");
            yazDeger(e.getValue(), sb, seviye + 1);
            if (++i < m.size()) {
                sb.append(',');
            }
            sb.append('\n');
        }
        girinti(sb, seviye);
        sb.append('}');
    }

    private static void yazDizi(List<?> l, StringBuilder sb, int seviye) {
        if (l.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append("[\n");
        for (int i = 0; i < l.size(); i++) {
            girinti(sb, seviye + 1);
            yazDeger(l.get(i), sb, seviye + 1);
            if (i < l.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        girinti(sb, seviye);
        sb.append(']');
    }

    private static void yazSayi(Number n, StringBuilder sb) {
        double d = n.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            sb.append("null");
        } else if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            sb.append((long) d);
        } else {
            sb.append(d);
        }
    }

    private static void yazMetin(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void girinti(StringBuilder sb, int seviye) {
        for (int i = 0; i < seviye; i++) {
            sb.append("  ");
        }
    }

    /** JSON metnini Java nesnelerine çevirir; bozuk metinde JsonHatasi fırlatır. */
    public static Object oku(String metin) {
        Ayristirici a = new Ayristirici(metin);
        a.bosluguAtla();
        Object sonuc = a.deger();
        a.bosluguAtla();
        if (!a.bitti()) {
            throw new JsonHatasi("Metnin sonunda beklenmeyen karakter (konum " + a.konum + ")");
        }
        return sonuc;
    }

    /** JSON okuma hatası. */
    public static class JsonHatasi extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public JsonHatasi(String mesaj) {
            super(mesaj);
        }
    }

    private static final class Ayristirici {
        private final String s;
        private int konum;

        Ayristirici(String s) {
            this.s = s;
        }

        boolean bitti() {
            return konum >= s.length();
        }

        void bosluguAtla() {
            while (konum < s.length() && Character.isWhitespace(s.charAt(konum))) {
                konum++;
            }
        }

        char bak() {
            if (bitti()) {
                throw new JsonHatasi("Metin beklenmedik sekilde bitti");
            }
            return s.charAt(konum);
        }

        Object deger() {
            bosluguAtla();
            char c = bak();
            switch (c) {
                case '{':
                    return nesne();
                case '[':
                    return dizi();
                case '"':
                    return metin();
                case 't':
                    return sabit("true", Boolean.TRUE);
                case 'f':
                    return sabit("false", Boolean.FALSE);
                case 'n':
                    return sabit("null", null);
                default:
                    return sayi();
            }
        }

        Map<String, Object> nesne() {
            Map<String, Object> m = new LinkedHashMap<>();
            konum++;
            bosluguAtla();
            if (!bitti() && bak() == '}') {
                konum++;
                return m;
            }
            while (true) {
                bosluguAtla();
                if (bak() != '"') {
                    throw new JsonHatasi("Anahtar metin olmali (konum " + konum + ")");
                }
                String anahtar = metin();
                bosluguAtla();
                if (bak() != ':') {
                    throw new JsonHatasi("Iki nokta bekleniyordu (konum " + konum + ")");
                }
                konum++;
                m.put(anahtar, deger());
                bosluguAtla();
                char c = bak();
                if (c == ',') {
                    konum++;
                } else if (c == '}') {
                    konum++;
                    return m;
                } else {
                    throw new JsonHatasi("Virgul veya kapanis bekleniyordu (konum " + konum + ")");
                }
            }
        }

        List<Object> dizi() {
            List<Object> l = new ArrayList<>();
            konum++;
            bosluguAtla();
            if (!bitti() && bak() == ']') {
                konum++;
                return l;
            }
            while (true) {
                l.add(deger());
                bosluguAtla();
                char c = bak();
                if (c == ',') {
                    konum++;
                } else if (c == ']') {
                    konum++;
                    return l;
                } else {
                    throw new JsonHatasi("Virgul veya kapanis bekleniyordu (konum " + konum + ")");
                }
            }
        }

        String metin() {
            konum++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (bitti()) {
                    throw new JsonHatasi("Kapanmamis metin");
                }
                char c = s.charAt(konum++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (bitti()) {
                    throw new JsonHatasi("Yarim kacis dizisi");
                }
                char k = s.charAt(konum++);
                switch (k) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (konum + 4 > s.length()) {
                            throw new JsonHatasi("Yarim unicode kacisi");
                        }
                        sb.append((char) Integer.parseInt(s.substring(konum, konum + 4), 16));
                        konum += 4;
                        break;
                    default:
                        throw new JsonHatasi("Gecersiz kacis dizisi");
                }
            }
        }

        Object sabit(String kelime, Object deger) {
            if (!s.startsWith(kelime, konum)) {
                throw new JsonHatasi("Gecersiz deger (konum " + konum + ")");
            }
            konum += kelime.length();
            return deger;
        }

        Double sayi() {
            int bas = konum;
            if (!bitti() && (bak() == '-' || bak() == '+')) {
                konum++;
            }
            while (!bitti()) {
                char c = s.charAt(konum);
                boolean rakam = c >= '0' && c <= '9';
                if (rakam || c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') {
                    konum++;
                } else {
                    break;
                }
            }
            if (bas == konum) {
                throw new JsonHatasi("Sayi bekleniyordu (konum " + konum + ")");
            }
            try {
                return Double.valueOf(s.substring(bas, konum));
            } catch (NumberFormatException e) {
                throw new JsonHatasi("Gecersiz sayi");
            }
        }
    }

    /** Verilen anahtarlardan ilk bulunanı metin olarak döndürür. */
    public static String metinAl(Map<?, ?> m, String varsayilan, String... anahtarlar) {
        for (String a : anahtarlar) {
            Object v = m.get(a);
            if (v instanceof String) {
                return (String) v;
            }
            if (v != null && !(v instanceof Map) && !(v instanceof List)) {
                return String.valueOf(v);
            }
        }
        return varsayilan;
    }

    /** Verilen anahtarlardan ilk bulunanı doğruluk değeri olarak döndürür. */
    public static boolean mantiksalAl(Map<?, ?> m, boolean varsayilan, String... anahtarlar) {
        for (String a : anahtarlar) {
            Object v = m.get(a);
            if (v instanceof Boolean) {
                return (Boolean) v;
            }
            if (v instanceof Number) {
                return ((Number) v).doubleValue() != 0;
            }
            if (v instanceof String) {
                String t = ((String) v).trim();
                if (t.equalsIgnoreCase("true") || t.equals("1") || t.equalsIgnoreCase("evet")) {
                    return true;
                }
                if (t.equalsIgnoreCase("false") || t.equals("0") || t.equalsIgnoreCase("hayir")) {
                    return false;
                }
            }
        }
        return varsayilan;
    }
}
