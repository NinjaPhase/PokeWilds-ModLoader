package com.pkmngen.mods.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.w3c.dom.Node;

public class ModClassPatcher extends ClassVisitor {
    private final Node patch;
    private String className;

    public ModClassPatcher(Node patch, ClassVisitor cv) {
        super(Opcodes.ASM7, cv);
        this.patch = patch;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        Node[] interfaceExtensions = AsmUtils.findTag(patch, "implement");
        if (interfaceExtensions.length > 0) {
            String[] newInterfaces = new String[interfaces.length + interfaceExtensions.length];
            System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
            for (int i = 0; i < interfaceExtensions.length; i++) {
                newInterfaces[interfaces.length + i] = AsmUtils.getAttribute(interfaceExtensions[i], "interface");
            }
            interfaces = newInterfaces;
        }
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        Node[] methodChanges = AsmUtils.findTag(patch, "method");
        for (Node n : methodChanges) {
            String mDescriptor = AsmUtils.getAttribute(n, "descriptor");
            if (mDescriptor == null || !mDescriptor.equals(name + descriptor)) {
                continue;
            }
            return new ModMethodPatcher(n, mv);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        Node[] getters = AsmUtils.findTag(this.patch, "getter");
        for (Node getter : getters) {
            addGetter(getter);
        }

        Node[] methods = AsmUtils.findTag(this.patch, "createmethod");
        for (Node n : methods) {
            addMethod(n);
        }

        Node[] fields = AsmUtils.findTag(this.patch, "field");
        for (Node n : fields) {
            addField(n);
        }

        super.visitEnd();
    }

    private void addField(Node n) {
        String name = AsmUtils.getAttribute(n, "name");
        String descriptor = AsmUtils.getAttribute(n, "descriptor");
        if (name == null || descriptor == null) {
            return;
        }

        FieldVisitor fieldVisitor = super.visitField(Opcodes.ACC_PUBLIC, name, descriptor, null, null);
        if (fieldVisitor != null) {
            fieldVisitor.visitEnd();
        }
    }

    private void addMethod(Node n) {
        String asm = n.getTextContent();
        String name = AsmUtils.getAttribute(n, "name");
        String descriptor = AsmUtils.getAttribute(n, "descriptor");
        String signature = AsmUtils.getAttribute(n, "signature");
        if (asm == null) {
            return;
        }

        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, signature, null);
        mv.visitCode();
        ModMethodPatcher.addASM(mv, asm);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void addGetter(Node n) {
        String forField = AsmUtils.getAttribute(n, "for");
        String gettype = AsmUtils.getAttribute(n, "gettype");
        String returnType = AsmUtils.getAttribute(n, "returntype");
        String cast = AsmUtils.getAttribute(n, "casttype");
        String name = n.getTextContent();
        if (forField == null || returnType == null || name == null) {
            return;
        }
        if (gettype == null) {
            gettype = returnType;
        }

        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, name, "()" + returnType, null, null);
        mv.visitCode();

        String op = "ARETURN";
        if (returnType.equals("I")) {
            op = "IRETURN";
        }

        if (cast == null) {
            ModMethodPatcher.addASM(mv, "ALOAD 0\n" +
                                        String.format("GETFIELD %s#%s %s\n", this.className, forField, gettype) +
                                        op + "\n");
        } else {
            ModMethodPatcher.addASM(mv, "ALOAD 0\n" +
                                        String.format("GETFIELD %s#%s %s\n", this.className, forField, gettype) +
                                        String.format("CHECKCAST %s\n", cast) +
                                        op + "\n");
        }
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }
}
