package com.gdetotut.jundo;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.gdetotut.jundo.UndoPacket.Result.Code.*;

/**
 * Class for control storing and restoring UndoStack's instances.
 * <p>Has features to tune these processes for various types of subject:
 * <ul>
 * <li>parameters {@link SubjInfo#id} and {@link SubjInfo#version} helps restore subject correctly
 * <li>function {@link Builder#onStoreManually} helps manually tune the storing process for non-serializable subjects
 * <li>function {@link #peek} helps to check whether this string has needed type for restore
 * <li>function {@link Peeker#restore} helps manually tune the restore process for non-serializable subjects
 * </ul>
 */
public class UndoPacket {

    /**
     * Implement this interface in {@link Builder#onStoreManually} to manually tune the storing process
     * for non-serializable subjects.
     */
    @FunctionalInterface
    public interface OnStoreManually {

        /**
         * Used in {@link Builder#onStoreManually} for manually convert {@link UndoStack#subj}
         * to {@link Serializable} form if required.
         *
         * @param subj {@link UndoStack#subj}
         * @return Converted {@link UndoStack#subj}
         * @see Builder#onStoreManually
         */
        Serializable store(Object subj);
    }

    /**
     * Implement this interface in {@link Peeker#restore} to manually tune the restoring process
     * for non-serializable subjects.
     */
    @FunctionalInterface
    public interface OnRestoreManually {

        /**
         * Should restore subject to its original form from processedSubj.
         *
         * @param processedSubj {@link UndoStack#subj} in the Serializable form that earlier processed
         *                      in the {@link Builder#onStoreManually}.
         * @param subjInfo      additional information stored with the stack
         * @return Restored {@link UndoStack#subj}.
         * @throws Exception If something goes wrong.
         */
        Object restore(Serializable processedSubj, SubjInfo subjInfo) throws Exception;
    }

    /**
     * Creator for new UndoStack. Is used to create UndoStack manually when there was error at restore.
     */
    @FunctionalInterface
    public interface OnUndoStackCreator {

        /**
         * @return newly created UndoStack
         */
        UndoStack createNew();
    }

    /**
     * Implement this interface in {@link #stack} to tune restored stack (i.e. to set local contexts).
     */
    @FunctionalInterface
    public interface OnProcessStack {

        /**
         * @param stack    restored stack.
         * @param subjInfo additional information stored with the stack.
         * @param result   code of unpacking procedure.
         */
        void process(UndoStack stack, SubjInfo subjInfo, Result result);
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
        private boolean zip = false;
        private OnStoreManually onStoreManually = null;

        /**
         * Adds key-value pair to map of additional information.
         * <p>If key exists replaces value.
         *
         * @param key   key for additional parameter.
         * @param value value of additional parameter.
         * @return Instance of Builder.
         */
        public Builder addExtra(String key, Serializable value) {
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
         * @return Instance of Builder.
         */
        public Builder zip() {
            this.zip = true;
            return this;
        }

        /**
         * Sets event handler for manual tune to store non-serializable subject.
         *
         * @param onStoreManually if not null user can manually convert non-serializable subject to {@link Serializable} form.
         * @return Instance of Builder.
         */
        public Builder onStoreManually(OnStoreManually onStoreManually) {
            this.onStoreManually = onStoreManually;
            return this;
        }

        /**
         * Terminal method for the storing chain process.
         * <p> Converts {@link UndoStack} to Base64 string using events (if set), gzip (if set) and other information.
         *
         * @return Converted stack in the Base64 form.
         * @throws Exception If something goes wrong.
         */
        public String store() throws Exception {

            InnerData data = new InnerData();
            data.stack = stack;

            // Обработчик есть: обрабатываем в строку
            //  иначе если не сериализуется ошибка
            //  иначе назначаем напрямую
            if (null != onStoreManually) {
                data.subj = onStoreManually.store(stack.getSubj());
                data.storedManually = true;
            } else if (!(stack.getSubj() instanceof Serializable)) {
                throw new Exception("UndoStack's subject not serializable");
            } else {
                data.subj = (Serializable) stack.getSubj();
                data.storedManually = false;
            }
            //~

            String dataPart = toBase64(data);

            SubjInfo subjInfo = new SubjInfo(id, version, extras);
            String subjInfoPart = toBase64(subjInfo);

            char[] ca = String.valueOf(subjInfoPart.length()).toCharArray();
            char[] caAddon = new char[HEADER_SIZE - ca.length];
            Arrays.fill(caAddon, HEADER_FILLER);
            String headerLenAsStr = new String(ca) + new String(caAddon);
            return headerLenAsStr + subjInfoPart + dataPart;
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

            if (zip) {
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
    private static class InnerData implements Serializable {
        UndoStack stack;
        Serializable subj;
        // True if subj handled via handler; otherwise false.
        boolean storedManually;
    }

    /**
     * Unpacking result.
     */
    public static class Result {

        public enum Code {
            RC_Success,        // unpack was successful
            RC_WrongCandidate, // Input string (candidate) was wrong
            RC_PeekRefused,    // Refusing on peek stage
            RC_NewStack        // Error at the restore step, the stack is created manually
        }

        public final Code code;
        public final String msg;

        Result(Code code, String msg) {
            this.code = code;
            this.msg = msg;
        }
    }

    //-------------------------------------------------------------------------------------------------

    public final Result result;

    /**
     * Additional information about {@link UndoStack#subj}.
     * Can be null if {@link Result} is RC_NewStack
     */
    public final SubjInfo subjInfo;

    /**
     * {@link UndoStack} itself.
     */
    public final UndoStack stack;

    //-------------------------------------------------------------------------------------------------

    /**
     * Initial method in the storing chain.
     *
     * @param stack   {@link UndoStack} to store. Required.
     * @param id      subject identifier. Highly recommend because allows restore stack more precisely.
     * @param version subject version. Highly recommend because allows migration in restore process.
     * @return {@link Builder} instance.
     */
    public static Builder prepare(UndoStack stack, String id, int version) {
        return new Builder(stack, id, version);
    }

    /**
     * Helper class to provide chain of restore methods.
     */
    public static class Peeker {

        public SubjInfo subjInfo;
        public final String candidate;
        public Result result;

        Peeker(String candidate, SubjInfo subjInfo, Result result) {
            this.candidate = candidate;
            this.subjInfo = subjInfo;
            this.result = result;
        }

        /**
         * Calls when some error occurs while automatic restoring process.
         *
         * @param creator callback function to create stack manually if error occurs while automatic restoring process.
         * @return manually restored stack.
         * @throws CreatorException when something goes wrong.
         */
        private UndoStack createNew(OnUndoStackCreator creator, Result result) throws CreatorException {
            UndoStack stack;
            if (creator == null) {
                throw new CreatorException(result.msg);
            } else {
                stack = creator.createNew();
                if (stack == null) {
                    throw new CreatorException("The creator returned null");
                }
            }
            return stack;
        }

        /**
         * Creates {@link UndoPacket} instance. Via parameter (if set) allows manually tune subject restore.
         *
         * @param handler event handler. Optional.
         * @param creator used when restoring fails. Recommend.
         * @return UndoPacket
         * @throws CreatorException Occurs when both restoring and creating failed.
         */
        public UndoPacket restore(OnRestoreManually handler, OnUndoStackCreator creator) throws CreatorException {

            UndoPacket packet;
            UndoStack stack;

            if (result.code != RC_Success) {
                stack = createNew(creator, result);
                // When is new, subjInfo is null
                packet = new UndoPacket(stack, null,
                        new Result(RC_NewStack,
                                result.msg == null ? result.code.name() : result.msg));
            } else {
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

                    InnerData data = (InnerData) ois.readObject();
                    stack = data.stack;
                    Object subj = data.subj;

                    if (null != handler) {
                        subj = handler.restore(data.subj, subjInfo);
                    } else if (data.storedManually) {
                        throw new Exception("when storing was handled then restoring need handler too");
                    }

                    stack.setSubj(subj);

                } catch (Exception e) {
                    subjInfo = null;
                    result = new Result(RC_NewStack, e.getLocalizedMessage());
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
     *
     * @param candidate input Base64 string.
     * @param p         predicate.
     * @return Helper Peeker's instance.
     */
    public static Peeker peek(String candidate, Predicate<SubjInfo> p) {

        Result result = new Result(RC_Success, null);

        if (null == candidate) {
            result = new Result(RC_WrongCandidate, "is null");
        } else if (candidate.length() < Builder.HEADER_SIZE) {
            result = new Result(RC_WrongCandidate, "too small size");
        }

        SubjInfo si = null;

        if (result.code == RC_Success) {
            String lenPart = Objects.requireNonNull(candidate).substring(0, Builder.HEADER_SIZE);
            lenPart = lenPart.substring(0, lenPart.indexOf(Builder.HEADER_FILLER));
            long len = Long.valueOf(lenPart);
            String subjInfoCandidate = candidate.substring(Builder.HEADER_SIZE, (int) (Builder.HEADER_SIZE + len));

            // This show if candidate has UndoStack inside it.
            try {
                si = (SubjInfo) fromBase64(subjInfoCandidate);
            } catch (Exception e) {
                result = new Result(RC_WrongCandidate, e.getLocalizedMessage());
            }

            if (result.code == RC_Success) {
                if (p != null && !p.test(si)) {
                    result = new Result(RC_PeekRefused, null);
                }
            }

        }

        // Here code is signal of whether candidate and obj are good or bad
        return new Peeker(candidate, si, result);
    }

    /**
     * Terminal optional method in restore chain.
     *
     * @param handler if not null user can tune finally stack (e.g. set local contexts).
     * @return UndoStack instance.
     */
    public UndoStack process(OnProcessStack handler) {
        if (null != handler) {
            handler.process(stack, subjInfo, result);
        }
        return stack;
    }

    private static Object fromBase64(String candidate) throws IOException, ClassNotFoundException {

        final byte[] data = Base64.getUrlDecoder().decode(candidate);
        final boolean zipped = (data[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                && (data[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));

        try (ObjectInputStream ois = zipped
                ? new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))
                : new ObjectInputStream(new ByteArrayInputStream(data))) {

            return ois.readObject();
        }
    }

    private UndoPacket(UndoStack stack, SubjInfo subjInfo, Result result) {
        this.stack = stack;
        this.subjInfo = subjInfo;
        this.result = result;
    }

}
