package net.ty.createcraftedbeginning.core.transaction;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ResourceTransaction {
    private final List<Participant<?>> participants = new ArrayList<>();

    public static <S> Participant<S> participant(BooleanSupplier validator, Supplier<S> snapshotter, BooleanSupplier executor, Consumer<S> restorer) {
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(snapshotter, "snapshotter");
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(restorer, "restorer");
        return new Participant<>() {
            @Override
            public boolean validate() {
                return validator.getAsBoolean();
            }

            @Override
            public S snapshot() {
                return snapshotter.get();
            }

            @Override
            public boolean execute() {
                return executor.getAsBoolean();
            }

            @Override
            public void restore(S snapshot) {
                restorer.accept(snapshot);
            }
        };
    }

    private static <S> CapturedParticipant capture(Participant<S> participant) {
        S snapshot = participant.snapshot();
        return new CapturedParticipant() {
            @Override
            public boolean execute() {
                return participant.execute();
            }

            @Override
            public void restore() {
                participant.restore(snapshot);
            }
        };
    }

    private static void rollback(List<CapturedParticipant> participants, int attemptedParticipants, @Nullable Throwable primaryFailure) {
        Throwable rollbackFailure = null;
        for (int index = attemptedParticipants - 1; index >= 0; index--) {
            try {
                participants.get(index).restore();
            } catch (RuntimeException | Error throwable) {
                if (primaryFailure != null) {
                    primaryFailure.addSuppressed(throwable);
                }
                else if (rollbackFailure == null) {
                    rollbackFailure = throwable;
                }
                else {
                    rollbackFailure.addSuppressed(throwable);
                }
            }
        }

        if (primaryFailure != null || rollbackFailure == null) {
            return;
        }

        if (rollbackFailure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw (Error) rollbackFailure;
    }

    public ResourceTransaction require(BooleanSupplier requirement) {
        Objects.requireNonNull(requirement, "requirement");
        return add(participant(requirement, () -> Boolean.TRUE, () -> true, ignored -> {}));
    }

    public <S> ResourceTransaction add(Participant<S> participant) {
        participants.add(Objects.requireNonNull(participant, "participant"));
        return this;
    }

    public boolean commit() {
        for (Participant<?> participant : participants) {
            if (participant.validate()) {
                continue;
            }
            
            return false;
        }

        List<CapturedParticipant> captured = new ArrayList<>(participants.size());
        for (Participant<?> participant : participants) {
            captured.add(capture(participant));
        }

        boolean committed = false;
        int attemptedParticipants = 0;
        Throwable failure = null;
        try {
            for (CapturedParticipant participant : captured) {
                attemptedParticipants++;
                if (!participant.execute()) {
                    return false;
                }
            }

            committed = true;
            return true;
        } catch (RuntimeException | Error throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            if (!committed) {
                rollback(captured, attemptedParticipants, failure);
            }
        }
    }

    public interface Participant<S> {
        boolean validate();

        S snapshot();

        boolean execute();

        void restore(S snapshot);
    }

    private interface CapturedParticipant {
        boolean execute();

        void restore();
    }
}
