package com.gdetotut.jundo;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class UndoPacket {

    /**
     * Для ручного управления упаковкой, например в случае, если субъект не имплементирует {@link java.io.Serializable}
     */
    public interface OnStore {
        String handle(Object subj);
    }

    /**
     * Для ручного управления распаковкой, например в случае, если субъект не имплементирует {@link java.io.Serializable}
     */
    public interface OnRestore {
        Object handle(Serializable processedSubj, SubjInfo subjInfo);
    }

    /**
     * Вспомогательный класс данных.
     * Используется для хранения
     */
    public static class SubjInfo implements Serializable {
        public final String id;
        public final int version;
        public final Map<String, Serializable> extras;

        public SubjInfo(String id, int version, Map<String, Serializable> extras) {
            this.id = id;
            this.version = version;
            this.extras = extras;
        }
    }

    public static class Builder {

        /**
         * Первоначально 8 байт (Long.BYTES in Java 1.8) но мало ли что )))
         */
        private static final int HEADER_SIZE = 40;
        private static final char HEADER_FILLER = 'Z';

        private final UndoStack stack;
        private final String id;
        private final int version;
        private Map<String, Serializable> extras = null;
        private boolean zipped = false;
        private OnStore onStore = null;

        public Builder(UndoStack stack, String id, int version) {
            if(null == stack) {
                throw new NullPointerException("stack");
            }
            this.stack = stack;
            this.id = id;
            this.version = version;
        }

        public Builder extra(String key, Serializable value) {
            if(null == key) {
                throw new NullPointerException("key");
            }
            if(null == extras) {
                extras = new TreeMap<>();
            }
            extras.put(key, value);
            return this;
        }

        public Builder zipped(boolean value) {
            this.zipped = value;
            return this;
        }

        public Builder onStore(OnStore onStore) {
            this.onStore = onStore;
            return this;
        }


        /**
         * <ul>
         *     <li>{@link SubjInfo} size zeroed up to 8 bytes: 8 bytes</li>
         *     <li>{@link SubjInfo} itself</li>
         *     <li>{@link Data} </li>
         * </ul>
         * Таким образом можно будет распаковать отдельно сперва {@link SubjInfo} для {@link #peek}
         * @return
         */
        public String store() throws Exception {

            Data data = new Data();
            data.stack = stack;

            // Обработчик есть: обрабатываем в строку
            //  иначе если не сериализуется ошибка
            //  иначе назначаем напрямую
            if(null != onStore) {
                data.subj = onStore.handle(stack.getSubj());
                data.subjHandled = true;
            }else if(!(stack.getSubj() instanceof Serializable)){
                throw new Exception("UndoStack's subject not serializable");
            }else {
                data.subj = (Serializable) stack.getSubj();
                data.subjHandled = false;
            }
            //~

            String dataPart = toBase64(data);

            SubjInfo subjInfo = new SubjInfo(id, version, extras);
            String subjInfoPart = toBase64(subjInfo);

            char[] ca = String.valueOf(subjInfoPart.length()).toCharArray();
            char[] caAddon = new char[HEADER_SIZE - ca.length];
            Arrays.fill(caAddon, HEADER_FILLER);
            String headerLenAsStr = new String(ca) + new String(caAddon);
            String res = headerLenAsStr + subjInfoPart + dataPart;
            return res;
        }

        private String toBase64(Serializable value) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(value);
            }

            if (zipped) {
                final ByteArrayOutputStream zippedBaos = new ByteArrayOutputStream();
                try (GZIPOutputStream gzip = new GZIPOutputStream(zippedBaos)) {
                    gzip.write(baos.toByteArray());
                }
                baos = zippedBaos;
            }
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        }

    }

    //----------------------------------------------------------------------------------------------------

    /**
     * Храним {@link UndoStack#subj} отдельно, так как в ряде случаев его придется вручную упаковывать.
     */
    private static class Data implements Serializable {
        UndoStack stack;
        Serializable subj;
        // True if subj handled via handler; otherwise false.
        boolean subjHandled;
    }

    //-------------------------------------------------------------------------------------------------

    public final boolean isExpected;

    public final UndoStack stack;

    //-------------------------------------------------------------------------------------------------

    public static Builder builder(UndoStack stack, String id, int version) {
        return new Builder(stack, id, version);
    }

    /**
     * Распаковывает и возвращает часть строки, как доп.данные о субъекте.
     * <p>Может сэкономить время и память при большом размере объекта.
     * В случае, если субъект ожидаемого типа, можно запросить распаковку; иначе можно не тратить ресурсы.
     * @param candidate
     * @return
     * @throws Exception
     */
    public static SubjInfo peek(String candidate) throws Exception {
        if(null == candidate) {
            throw new NullPointerException("candidate");
        }
        if(candidate.length() < Builder.HEADER_SIZE) {
            throw new Exception("too small size");
        }

        String lenPart = candidate.substring(0, Builder.HEADER_SIZE);
        lenPart = lenPart.substring(0, lenPart.indexOf(Builder.HEADER_FILLER));
        long len = Long.valueOf(lenPart);
        String subjInfoCandidate = candidate.substring(Builder.HEADER_SIZE, (int)(Builder.HEADER_SIZE + len));
        Object obj = fromBase64(subjInfoCandidate);
        return (SubjInfo)obj;
    }

    /**
     * Распаковывает и возвращает пакет.
     * @param candidate
     * @return
     * @throws Exception
     */
    public static UndoPacket restore(String candidate, OnRestore handler) throws Exception {
        if(null == candidate) {
            throw new NullPointerException("candidate");
        } else {

            String lenPart = candidate.substring(0, Builder.HEADER_SIZE);
            lenPart = lenPart.substring(0, lenPart.indexOf(Builder.HEADER_FILLER));
            long len = Long.valueOf(lenPart);
            String dataCandidate = candidate.substring((int)(Builder.HEADER_SIZE + len));

            final byte[] arr = Base64.getUrlDecoder().decode(dataCandidate);
            final boolean zipped = (arr[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                    && (arr[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));

            try (ObjectInputStream ois = zipped
                    ? new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(arr)))
                    : new ObjectInputStream(new ByteArrayInputStream(arr))) {

                boolean isExp = true;
                Data data = (Data) ois.readObject();
                UndoStack stack = data.stack;
                Object subj = data.subj;

                // Если хендлер назначен, надо его использовать
                if(null != handler) {
                    SubjInfo subjInfo = peek(candidate);
                    subj = handler.handle(data.subj, subjInfo);
                }else if(data.subjHandled) {
                    throw new Exception("need subject handler");
                }

                if(null == subj) {
                    isExp = false;
                    subj = new Object();
                }
                stack.setSubj(subj);
                UndoPacket packet = new UndoPacket(stack, isExp);
                return packet;
            }
        }
    }

    private static Object fromBase64(String candidate) throws IOException, ClassNotFoundException {
        if(null == candidate) {
            throw new NullPointerException("candidate");
        }

        final byte[] data = Base64.getUrlDecoder().decode(candidate);
        final boolean zipped = (data[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                && (data[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));

        try (ObjectInputStream ois = zipped
                ? new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))
                : new ObjectInputStream(new ByteArrayInputStream(data))) {

            boolean isExp = true;
            Object obj = ois.readObject();
            return obj;
        }

    }

    private UndoPacket(UndoStack stack, boolean isExpected) {
        this.stack = stack;
        this.isExpected = isExpected;
    }

}
