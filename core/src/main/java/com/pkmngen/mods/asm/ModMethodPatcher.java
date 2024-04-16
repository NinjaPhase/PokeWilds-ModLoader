package com.pkmngen.mods.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModMethodPatcher extends MethodVisitor {
    private int ins;

    private final Node patch;

    public ModMethodPatcher(Node patch, MethodVisitor methodVisitor) {
        super(Opcodes.ASM7, methodVisitor);
        this.patch = patch;
    }

    @Override
    public void visitCode() {
        this.ins = 0;
        super.visitCode();
    }

    @Override
    public void visitInsn(int opcode) {
        this.checkInsert(true);

        this.ins++;
        super.visitInsn(opcode);
        this.checkInsert(false);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        this.checkInsert(true);

        this.ins++;
        super.visitIntInsn(opcode, operand);
        this.checkInsert(false);
    }

    @Override
    public void visitVarInsn(int opcode, int varIndex) {
        this.checkInsert(true);

        this.ins++;
        super.visitVarInsn(opcode, varIndex);
        this.checkInsert(false);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        this.checkInsert(true);

        this.ins++;
        super.visitTypeInsn(opcode, type);
        this.checkInsert(false);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        this.checkInsert(true);

        this.ins++;
        super.visitFieldInsn(opcode, owner, name, descriptor);
        this.checkInsert(false);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        this.checkInsert(true);

        this.ins++;
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        this.checkInsert(false);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        this.checkInsert(true);

        this.ins++;
        super.visitJumpInsn(opcode, label);
        this.checkInsert(false);
    }

    @Override
    public void visitLdcInsn(Object value) {
        this.checkInsert(true);

        this.ins++;
        super.visitLdcInsn(value);
        this.checkInsert(false);
    }

    @Override
    public void visitIincInsn(int varIndex, int increment) {
        this.checkInsert(true);

        this.ins++;
        super.visitIincInsn(varIndex, increment);
        this.checkInsert(false);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        this.checkInsert(true);

        this.ins++;
        super.visitTableSwitchInsn(min, max, dflt, labels);
        this.checkInsert(false);
    }

    public void checkInsert(boolean before) {
        Node[] asmTags = AsmUtils.findTag(this.patch, "asm");
        for (Node n : asmTags) {
            String idxAttr = AsmUtils.getAttribute(n, before ? "before" : "after");
            if (idxAttr == null) {
                continue;
            }
            int idx = Integer.parseInt(idxAttr);
            if (idx != this.ins) {
                continue;
            }
            this.insertCode(n);
        }
    }

    public void insertCode(Node n) {
        if (n.getTextContent() == null) {
            return;
        }
        ModMethodPatcher.addASM(this.mv, n.getTextContent());
    }

    public static void addASM(MethodVisitor mv, String asm) {
        String[] lines = asm.trim().split("\n");
        for (String line : lines) {
            line = line.trim();
            String[] ops = line.split(" ", 2);
            if (ops.length == 0) {
                continue;
            }
            String opCode = ops[0];
            Matcher m;
            switch(opCode) {
                case "RETURN":
                case "IRETURN":
                case "ARETURN":
                case "AASTORE":
                case "ICONST_0":
                case "ACONST_NULL":
                case "DUP":
                    mv.visitInsn(getOpcodeFromName(opCode));
                    break;
                case "ILDC":
                    mv.visitLdcInsn(Integer.parseInt(ops[1]));
                    break;
                case "CHECKCAST":
                case "ANEWARRAY":
                case "NEW":
                    mv.visitTypeInsn(getOpcodeFromName(opCode), ops[1]);
                    break;
                case "ILOAD":
                case "ALOAD":
                case "ASTORE":
                    int varIndex = Integer.parseInt(ops[1]);
                    mv.visitVarInsn(getOpcodeFromName(opCode), varIndex);
                    break;
                case "GETSTATIC":
                case "GETFIELD":
                case "PUTFIELD":
                    m = Pattern.compile("(.+)#(.+) (.+)", Pattern.MULTILINE).matcher(ops[1]);
                    if (!m.find()) {
                        throw new RuntimeException("invalid expression " + line);
                    }
                    mv.visitFieldInsn(getOpcodeFromName(opCode), m.group(1), m.group(2), m.group(3));
                    break;
                case "INVOKEVIRTUAL":
                case "INVOKESTATIC":
                case "INVOKESPECIAL":
                    m = Pattern.compile("(.+)#(.+)(\\(.+)", Pattern.MULTILINE).matcher(ops[1]);
                    if (!m.find()) {
                        throw new RuntimeException("invalid expression " + line);
                    }
                    mv.visitMethodInsn(getOpcodeFromName(opCode), m.group(1), m.group(2), m.group(3), false);
                    break;
                case "":
                    break;
                default:
                    System.err.println("invalid operation " + opCode);
                    break;
            }
        }
    }

    public static int getOpcodeFromName(String name) {
        try {
            return (int) Opcodes.class.getDeclaredField(name).get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
