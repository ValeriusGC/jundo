package com.gdetotut.jundo;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class for control storing and restoring UndoStack's instances.
 * <p>Has features to tune these processes for various types of subject:
 * <ul>
 *     <li>parameters {@link SubjInfo#id} and {@link SubjInfo#version} helps restore subject correctly
 *     <li>function {@link Builder#onStore} helps manually tune the storing process for non-serializable subjects
 *     <li>function {@link #peek} helps to check whether this string has needed type for restore
 *     <li>function {@link Peeker#restore} helps manually tune the restore process for non-serializable subjects
 * </ul>
 */
public class UndoPacket {

    /**
     * Implement this interface in {@link Builder#onStore} to manually tune the storing process for non-serializable subjects.
     */
    @FunctionalInterface
    public interface OnStore {

        /**
         * Used in {@link Builder#onStore} for manually convert {@link UndoStack#subj} to {@link Serializable} form if required.
         * @param subj {@link UndoStack#subj}
         * @return Converted {@link UndoStack#subj}
         * @see Builder#onStore
         */
        Serializable handle(Object subj);
    }

    /**
     * Implement this interface in {@link Peeker#restore} to manually tune the restoring process for non-serializable subjects.
     */
    @FunctionalInterface
    public interface OnRestore {

        /**
         *
         * @param processedSubj {@link UndoStack#subj} in the Serializable form that earlier processed in the {@link Builder#onStore}.
         * @param subjInfo additional information stored with the stack
         * @return Restored {@link UndoStack#subj}.
         * @throws Exception If something goes wrong.
         */
        Object handle(Serializable processedSubj, SubjInfo subjInfo) throws Exception;
    }

    /**
     * Creator for new UndoStack. Is used when was error at restore.
     */
    @FunctionalInterface
    public interface OnCreate {
        UndoStack createNew();
    }

    /**
     * Implement this interface in {@link #stack} to tune restored stack (i.e. to set local contexts).
     */
    @FunctionalInterface
    public interface OnPrepareStack {

        /**
         * @param stack restored stack.
         * @param subjInfo additional information stored with the stack
         */
        void apply(UndoStack stack, SubjInfo subjInfo);
    }

    /**
     * Additional subject's information stored with the stack.
     */
    public static class SubjInfo implements Serializable {

        /**
         * Identifier for the subject.
         * <p>It is a good practice to save it with stack. It helps restore things correctly.
         */
        public final String id;

        /**
         * Subject's version.
         * <p>It is a good practice to save it with stack. It helps restore things correctly and make migration
         * if necessary.
         */
        public final int version;

        /**
         * Some extra information. Optional.
         */
        public final Map<String, Serializable> extras;

        public SubjInfo(String id, int version, Map<String, Serializable> extras) {
            this.id = id;
            this.version = version;
            this.extras = extras;
        }
    }

    /**
     * Builder to make store handy.
     */
    public static class Builder {

        private static final int HEADER_SIZE = 40;
        private static final char HEADER_FILLER = 'Z';

        private final UndoStack stack;
        private final String id;
        private final int version;
        private Map<String, Serializable> extras = null;
        private boolean zipped = false;
        private OnStore onStore = null;

        /**
         * Adds key-value pair to map of additional information.
         * <p>If key exists replaces value.
         *
         * @param key key for additional parameter.
         * @param value value of additional parameter.
         * @return Instance of Builder.
         */
        public Builder extra(String key, Serializable value) {
            if (key == null) {
                throw new NullPointerException("key");
            }
            if (extras == null) {
                extras = new TreeMap<>();
            }
            extras.put(key, value);
            return this;
        }

        /**
         * Sets flag for gzip when store.
         *
         * @param value true if wanna to gzip; otherwise pass false.
         * @return Instance of Builder.
         */
        public Builder zipped(boolean value) {
            this.zipped = value;
            return this;
        }

        /**
         * Sets event handler for manual tune to store non-serializable subject.
         *
         * @param onStore if not null user can manually convert non-serializable subject to {@link Serializable} form.
         * @return Instance of Builder.
         */
        public Builder onStore(OnStore onStore) {
            this.onStore = onStore;
            return this;
        }

        /**
         * Terminal method for the storing chain process.
         * <p> Converts {@link UndoStack} to Base64 string using events (if set), gzip (if set) and other information.
         * @return Converted stack in the Base64 form.
         * @throws Exception If something goes wrong.
         */
        public String store() throws Exception {

            Data data = new Data();
            data.stack = stack;

            // Обработчик есть: обрабатываем в строку
            //  иначе если не сериализуется ошибка
            //  иначе назначаем напрямую
            if (null != onStore) {
                data.subj = onStore.handle(stack.getSubj());
                data.subjHandled = true;
            } else if (!(stack.getSubj() instanceof Serializable)) {
                throw new Exception("UndoStack's subject not serializable");
            } else {
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

        private Builder(UndoStack stack, String id, int version) {
            if (null == stack) {
                throw new NullPointerException("stack");
            }
            this.stack = stack;
            this.id = id;
            this.version = version;
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
     * Helper structure to store {@link UndoStack}
     */
    private static class Data implements Serializable {
        UndoStack stack;
        Serializable subj;
        // True if subj handled via handler; otherwise false.
        boolean subjHandled;
    }


    public static class Result {
        public final UndoPacket.UnpackResult result;
        public final String msg;

        public Result(UndoPacket.UnpackResult result, String msg) {
            this.result = result;
            this.msg = msg;
        }
    }

    //-------------------------------------------------------------------------------------------------

    // TODO: 01.02.18 Рассортировать, убрать лишнее
    public enum UnpackResult {
        UPR_Success, // unpack was successful
        UPR_WrongCandidate, // Input string (candidate) was wrong
        UPR_PeekRefused,    // Refusing on peek stage
        UPR_NewStack        // Error at restore step
    }

    public final Result result;

    /**
     * Additional information about {@link UndoStack#subj}.
     * Can be null if {@link Result} is UPR_NewStack
     */
    public final SubjInfo subjInfo;

    /**
     * {@link UndoStack} itself. Available only through {@link UndoPacket#stack} method.
     */
    private final UndoStack stack;

    //-------------------------------------------------------------------------------------------------

    /**
     * Initial method in the storing chain.
     * @param stack {@link UndoStack} to store. Required.
     * @param id subject identifier. Highly recommend because allows restore stack more precisely.
     * @param version subject version. Highly recommend because allows migration in restore process.
     * @return {@link Builder} instance.
     */
    public static Builder make(UndoStack stack, String id, int version) {
        return new Builder(stack, id, version);
    }

    /**
     * Helper class to provide chain of restore methods.
     */
    public static class Peeker {

        public SubjInfo subjInfo;
        public final String candidate;
        public UndoPacket.Result result;

        Peeker(String candidate, SubjInfo subjInfo, UndoPacket.Result result) {
            this.candidate = candidate;
            this.subjInfo = subjInfo;
            this.result = result;
        }

        /**
         * Calls when is some error on restore state.
         * @param creator
         * @return
         * @throws CreatorException
         */
        private UndoStack createNew(OnCreate creator, UndoPacket.Result reason) throws CreatorException {
            UndoStack stack;
            if(creator == null) {
                throw new CreatorException(reason.msg);
            }else {
                stack = creator.createNew();
                if(stack == null) {
                    throw new CreatorException("the creator returned null");
                }
            }
            return stack;
        }

        /**
         * @param handler event handler. Optional.
         * @return {@link UndoPacket} instance.
         * @throws Exception If something goes wrong.
         */
        /**
         * Creates {@link UndoPacket} instance. Via parameter (if set) allows manually tune subject restore.
         *
         * @param handler event handler. Optional.
         * @param creator used when restoring fails. Recommend.
         * @return UndoPacket
         * @throws CreatorException Occurs when both restoring and creating failed.
         */
        public UndoPacket restore(OnRestore handler, OnCreate creator) throws CreatorException {

            UndoPacket packet;
            UndoStack stack;

            if(result.result != UnpackResult.UPR_Success) {
                stack = createNew(creator, result);
                // When is new, subjInfo is null
                packet = new UndoPacket(stack, null, new Result(UnpackResult.UPR_NewStack, result.msg));
            } else{


                String lenPart = candidate.substring(0, Builder.HEADER_SIZE);
                lenPart = lenPart.substring(0, lenPart.indexOf(Builder.HEADER_FILLER));
                long len = Long.valueOf(lenPart);
                String dataCandidate = candidate.substring((int) (Builder.HEADER_SIZE + len));

                final byte[] arr = Base64.getUrlDecoder().decode(dataCandidate);
                final boolean zipped = (arr[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                        && (arr[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));

                // 1. Try to restore
                try (ObjectInputStream ois = zipped
                        ? new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(arr)))
                        : new ObjectInputStream(new ByteArrayInputStream(arr))) {

                    Data data = (Data) ois.readObject();
                    stack = data.stack;
                    Object subj = data.subj;

                    // Если хендлер назначен, надо его использовать
                    if (null != handler) {
                        subj = handler.handle(data.subj, subjInfo);
                    } else if (data.subjHandled) {
                        throw new Exception("when storing was handled then restoring need handler too");
                    }

                    stack.setSubj(subj);

                } catch (Exception e) {
                    subjInfo = null;
                    result = new Result(UnpackResult.UPR_NewStack, e.getLocalizedMessage());
                    stack = createNew(creator, result);
                }

                packet = new UndoPacket(stack, subjInfo, result);
            }

            return packet;
        }

    }

    /**
     * Initial method in restore chain.
     * <p>At this moment only {@link SubjInfo} element is restored and using predicate function user can solve whether we need restore entire {@link UndoStack}.
     * It is possible we have wrong input string so we can save resources when interrupt process on time.
     * @param candidate input Base64 string.
     * @param p predicate.
     * @return Helper Peeker's instance.
     * @throws Exception If something goes wrong.
     */
    public static Peeker peek(String candidate, Predicate<SubjInfo> p) {

        UndoPacket.Result result = new Result(UnpackResult.UPR_Success, null);

        if (null == candidate) {
            result = new Result(UnpackResult.UPR_WrongCandidate, "is null");
        } else if (candidate.length() < Builder.HEADER_SIZE ){
            result = new Result(UnpackResult.UPR_WrongCandidate, "too small size");
        }

        SubjInfo obj = null;

        if(result.result == UnpackResult.UPR_Success) {
            String lenPart = candidate.substring(0, Builder.HEADER_SIZE);
            lenPart = lenPart.substring(0, lenPart.indexOf(Builder.HEADER_FILLER));
            long len = Long.valueOf(lenPart);
            String subjInfoCandidate = candidate.substring(Builder.HEADER_SIZE, (int) (Builder.HEADER_SIZE + len));

            // This show if candidate has UndoStack inside it.
            try {
                obj = (SubjInfo) fromBase64(subjInfoCandidate);
            }catch (Exception e) {
                result = new Result(UnpackResult.UPR_WrongCandidate, e.getLocalizedMessage());
            }


            if(result.result == UnpackResult.UPR_Success) {
                if(p != null && !p.test(obj)) {
                    result = new Result(UnpackResult.UPR_PeekRefused, null);
                }
            }

        }

        // Here result is signal of whether candidate and obj are good or bad
        Peeker peeker = new Peeker(candidate, obj, result);
        return peeker;
    }

    /**
     * Terminal method in restore chain.
     * @param handler if not null user can tune finally stack (e.g. set local contexts).
     * @return UndoStack instance.
     */
    public UndoStack stack(OnPrepareStack handler) {
        if (null != handler) {
            handler.apply(stack, subjInfo);
        }
        return stack;
    }

    private static Object fromBase64(String candidate) throws IOException, ClassNotFoundException {
//      It can not be null because chacked when called
//        if (null == candidate) {
//            throw new NullPointerException("candidate");
//        }

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

    private UndoPacket(UndoStack stack, SubjInfo subjInfo, UndoPacket.Result result) {
        this.stack = stack;
        this.subjInfo = subjInfo;
        this.result = result;
    }

}
