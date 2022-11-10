package org.optaplanner.jpyinterpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.optaplanner.jpyinterpreter.util.JumpUtils;

public class PythonExceptionTable {
    private final List<ExceptionBlock> blockList;

    public PythonExceptionTable() {
        this.blockList = new ArrayList<>();
    }

    public void addEntry(PythonVersion pythonVersion, int blockStartInstructionInclusive, int blockEndInstructionInclusive,
            int targetByteOffset, int stackDepth, boolean pushLastIndex) {
        blockList.add(
                new ExceptionBlock(JumpUtils.getInstructionIndexForByteOffset(blockStartInstructionInclusive, pythonVersion),
                        JumpUtils.getInstructionIndexForByteOffset(blockEndInstructionInclusive, pythonVersion) + 1,
                        JumpUtils.getInstructionIndexForByteOffset(targetByteOffset, pythonVersion),
                        stackDepth, pushLastIndex));
    }

    public List<ExceptionBlock> getEntries() {
        return blockList;
    }

    public boolean containsJumpTarget(int target) {
        return blockList.stream().anyMatch(block -> block.targetInstruction == target);
    }

    public Set<Integer> getJumpTargetSet() {
        return blockList.stream().map(ExceptionBlock::getTargetInstruction).collect(Collectors.toSet());
    }

    public Set<Integer> getJumpTargetForStartSet(int start) {
        return blockList.stream()
                .filter(exceptionBlock -> exceptionBlock.getBlockStartInstructionInclusive() == start)
                .map(ExceptionBlock::getTargetInstruction)
                .collect(Collectors.toSet());
    }

    public List<ExceptionBlock> getInnerExceptionBlockList(int start, Set<Integer> possibleJumpTargetSet) {
        return blockList.stream()
                .filter(exceptionBlock -> exceptionBlock.getBlockStartInstructionInclusive() == start ||
                        exceptionBlock.containsAnyTargetInSet(possibleJumpTargetSet))
                .collect(Collectors.toList());
    }

    public Set<Integer> getStartPositionSet() {
        return blockList.stream().map(ExceptionBlock::getBlockStartInstructionInclusive).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return blockList.stream().map(ExceptionBlock::toString)
                .collect(Collectors.joining("\n    ", "ExceptionTable:\n    ", ""));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonExceptionTable that = (PythonExceptionTable) o;
        return Objects.equals(blockList, that.blockList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockList);
    }

    public static class ExceptionBlock {
        /**
         * The first instruction in the try block
         */
        final int blockStartInstructionInclusive;

        /**
         * The first instruction after the try block
         */
        final int blockEndInstructionExclusive;

        /**
         * Where to jump to if an exception happens
         */
        final int targetInstruction;

        /**
         * The expected stack size for this exception
         */
        final int stackDepth;

        /**
         * If true, push the offset that the exception was raised at before pushing the exception.
         */
        final boolean pushLastIndex;

        public ExceptionBlock(int blockStartInstructionInclusive, int blockEndInstructionExclusive, int targetInstruction,
                int stackDepth, boolean pushLastIndex) {
            this.blockStartInstructionInclusive = blockStartInstructionInclusive;
            this.blockEndInstructionExclusive = blockEndInstructionExclusive;
            this.targetInstruction = targetInstruction;
            this.stackDepth = stackDepth;
            this.pushLastIndex = pushLastIndex;
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder().append(blockStartInstructionInclusive).append(" to ")
                    .append(blockEndInstructionExclusive)
                    .append(" -> ").append(targetInstruction).append(" [").append(stackDepth).append("]");
            if (pushLastIndex) {
                out.append(" lasti");
            }
            return out.toString();
        }

        public int getBlockStartInstructionInclusive() {
            return blockStartInstructionInclusive;
        }

        public int getBlockEndInstructionExclusive() {
            return blockEndInstructionExclusive;
        }

        public int getTargetInstruction() {
            return targetInstruction;
        }

        public int getStackDepth() {
            return stackDepth;
        }

        public boolean isPushLastIndex() {
            return pushLastIndex;
        }

        public boolean containsAnyTargetInSet(Set<Integer> possibleJumpTargetSet) {
            for (int target : possibleJumpTargetSet) {
                if (target <= blockStartInstructionInclusive && target < blockEndInstructionExclusive) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ExceptionBlock that = (ExceptionBlock) o;
            return blockStartInstructionInclusive == that.blockStartInstructionInclusive
                    && blockEndInstructionExclusive == that.blockEndInstructionExclusive
                    && targetInstruction == that.targetInstruction && stackDepth == that.stackDepth
                    && pushLastIndex == that.pushLastIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockStartInstructionInclusive, blockEndInstructionExclusive, targetInstruction, stackDepth,
                    pushLastIndex);
        }
    }
}
