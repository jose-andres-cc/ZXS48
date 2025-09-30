/////////////////////////////////////////////////////////////////////////
// Z80.java
// Implementation of a Z80 processor in Java
// Copyright (c) 2025, Jose Andres Calvo Conde
//
// Fuentes documentales
// - Z80 CPU Product Specifications: https://www.zilog.com/docs/z80/ps0178.pdf
// - Z80 Undocumented Features: http://www.z80.info/z80undoc3.txt
// - (continuara)

public class Z80 {

    // Componentes de la clase
    private Z80Registers regs;
    private Z80ALU alu;
    private Z80Bus dataBus;
    private int tStates;

    /////////////////////////////////////////////////////////////////////////
    // Parity precomputed table
    // (even parity == true / odd parity == false)

    static boolean[] parityTable = {
            true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
            false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
            false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
            true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
            false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
            true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
            true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
            false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
            false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
            true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
            true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
            false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
            true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true,
            false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
            false, true, true, false, true, false, false, true, true, false, false, true, false, true, true, false,
            true, false, false, true, false, true, true, false, false, true, true, false, true, false, false, true
    };

    // Constructor
    public Z80() {
        regs = new Z80Registers();
        regs.reset();
        alu = new Z80ALU(regs, parityTable);
        // Inicializar el contador de tStates
        tStates = 0;
    }

    // Método para emular la ejecución de una instrucción
    public void execInst() {

        // Obtener la siguiente instrucción (fetch)
        byte op = dataBus.memReadOpCode(regs.getPC() & 0xFFFF);

        regs.setPC((short) (regs.getPC() + 1)); // Incrementa el contador de programa

        // Incrementa el registro de contador de instrucciones R
        REFRESH_CYCLE();
        regs.setQF(false);
        // Decodificar y ejecutar la instrucción
        switch (op & 0xFF) {

            // NOP
            case 0x00:
                tStates += 4;
                break;

            // LD BC,NN
            case 0x01:
                tStates += 10;
                regs.C.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                regs.B.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // LD (BC),A
            case 0x02:
                tStates += 7;
                dataBus.memWrite((regs.BC.getValue16() & 0xFFFF), (byte) regs.A.getValue());
                regs.setZ((byte) ((regs.C.getValue() + 1) & 0xFF));
                regs.setW(regs.A.getValue());

                break;

            // INC BC
            case 0x03:
                tStates += 6;
                regs.BC.setValue((short) (regs.BC.getValue16() + 1));
                break;

            // inc b
            case 0x04:
                tStates += 4;
                alu.INC_R8(regs.B);
                break;

            // dec b
            case 0x05:
                tStates += 4;
                alu.DEC_R8(regs.B);
                break;

            // ld b,N
            case 0x06:
                tStates += 7;
                regs.B.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // rlca
            case 0x07:
                tStates += 4; {
                byte aux = (byte) regs.A.getValue();
                regs.A.setValue((byte) (((aux << 1) & 0xfe) | ((aux >> 7) & 0x01)));
                regs.setHF(false);
                regs.setNF(false);
                regs.setCF((aux & 0x80) != 0);
                regs.setF3((regs.A.getValue() & 0x08) != 0);
                regs.setF5((regs.A.getValue() & 0x20) != 0);

            }
                break;

            // ex af,af'
            case 0x08:
                tStates += 4; {
                short af = regs.getAF();
                regs.setAF(regs.altAF.getValue16());
                regs.altAF.setValue(af);
            }
                break;

            // add hl,bc
            case 0x09:
                tStates += 11;
                alu.ADD_R16(regs.HL, regs.BC.getValue16());
                break;

            // ld a,(bc)
            case 0x0A:
                tStates += 7;
                regs.A.setValue((byte) dataBus.memRead((regs.BC.getValue16() & 0xFFFF)));
                regs.setWZ((short) (regs.BC.getValue16() + 1));
                break;

            // dec bc
            case 0x0B:
                tStates += 6;
                regs.BC.setValue((short) (regs.BC.getValue16() - 1));
                break;

            // inc c
            case 0x0C:
                tStates += 4;
                alu.INC_R8(regs.C);
                break;

            // dec c
            case 0x0D:
                tStates += 4;
                alu.DEC_R8(regs.C);
                break;

            // ld c,N
            case 0x0E:
                tStates += 7;
                regs.C.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // rrca
            case 0x0F:
                tStates += 4; {
                byte aux = (byte) regs.A.getValue();
                regs.setCF((aux & 0x01) != 0);
                regs.A.setValue((byte) (((aux >> 1) & 0x7F) | (((aux & 0x01) << 7) & 0x80)));
                regs.setHF(false);
                regs.setNF(false);
                regs.setF3((regs.A.getValue() & 0x08) != 0);
                regs.setF5((regs.A.getValue() & 0x20) != 0);
            }
                break;

            // DJNZ
            case 0x10:
                regs.B.setValue((byte) (regs.B.getValue() - 1));
                tStates += 8;
                if (regs.B.getValue() != 0) {
                    tStates += 5;
                    regs.setPC((short) (regs.getPC() + (byte) dataBus.memRead(regs.getPC() & 0xFFFF) + 1));
                    regs.setWZ(regs.getPC());
                } else {
                    regs.setPC((short) (regs.getPC() + 1));
                }
                break;

            // LD DE,NN
            case 0x11:
                tStates += 10;
                regs.E.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                regs.D.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // LD (DE),A
            case 0x12:
                tStates += 7;
                dataBus.memWrite((regs.DE.getValue16() & 0xFFFF), (byte) regs.A.getValue());
                regs.setZ((byte) ((regs.E.getValue() + 1) & 0xFF));
                regs.setW(regs.A.getValue());
                break;

            // INC DE
            case 0x13:
                tStates += 6;
                regs.DE.setValue((short) (regs.DE.getValue16() + 1));
                break;

            // INC D
            case 0x14:
                tStates += 4;
                alu.INC_R8(regs.D);
                break;

            // DEC D
            case 0x15:
                tStates += 4;
                alu.DEC_R8(regs.D);
                break;

            // LD D,N
            case 0x16:
                tStates += 7;
                regs.D.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // RLA
            case 0x17:
                tStates += 4; {
                byte aux = (byte) regs.A.getValue();
                regs.A.setValue((byte) (((regs.A.getValue() << 1) & 0xfe) | (regs.getCF() ? 0x01 : 0x00)));
                regs.setCF((aux & 0x80) != 0);
                regs.setNF(false);
                regs.setHF(false);
                regs.setF3((regs.A.getValue() & 0x008) != 0);
                regs.setF5((regs.A.getValue() & 0x020) != 0);
            }
                break;

            // JR D
            case 0x18:
                tStates += 12;
                regs.setPC((short) (regs.getPC() + 1 + (byte) dataBus.memRead(regs.getPC() & 0xFFFF)));
                regs.setWZ(regs.getPC());
                break;

            // ADD HL,DE
            case 0x19:
                tStates += 11;
                alu.ADD_R16(regs.HL, regs.DE.getValue16());
                break;

            // LD A,(DE)
            case 0x1A:
                tStates += 7;
                regs.A.setValue(dataBus.memRead(regs.DE.getValue16()));
                regs.setWZ((short) (regs.DE.getValue16() + 1));
                break;

            // DEC DE
            case 0x1B:
                tStates += 6;
                regs.DE.setValue((short) (regs.DE.getValue16() - 1));
                break;

            // INC E
            case 0x1C:
                tStates += 4;
                alu.INC_R8(regs.E);
                break;

            // DEC E
            case 0x1D:
                tStates += 4;
                alu.DEC_R8(regs.E);
                break;

            // LD E,N
            case 0x1E:
                tStates += 7;
                regs.E.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // RRA
            case 0x1F:
                tStates += 4; {
                byte aux = (byte) regs.A.getValue();
                regs.A.setValue((byte) (((regs.A.getValue() >> 1) & 0x7f) | (regs.getCF() ? 0x80 : 0x00)));
                regs.setCF((aux & 0x01) != 0);
                regs.setNF(false);
                regs.setHF(false);
                regs.setF3((regs.A.getValue() & 0x008) != 0);
                regs.setF5((regs.A.getValue() & 0x020) != 0);
            }
                break;

            // JR NZ, D
            case 0x20:
                tStates += 7;
                if (!regs.getZF()) {
                    tStates += 5;
                    regs.setPC((short) (regs.getPC() + (byte) dataBus.memRead(regs.getPC() & 0xFFFF)));
                    regs.setWZ((short) ((regs.getPC() + 1) & 0xFFFF));
                }
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // LD HL, NN
            case 0x21:
                tStates += 10;
                regs.L.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                regs.H.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // LD (NN), HL
            case 0x22:
                tStates += 16;
                write16(read16(regs.getPC()), regs.HL.getValue16());
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // INC HL
            case 0x23:
                tStates += 6;
                regs.HL.setValue((short) (regs.HL.getValue16() + 1));
                break;

            // INC H
            case 0x24:
                tStates += 4;
                alu.INC_R8(regs.H);
                break;

            // DEC H
            case 0x25:
                tStates += 4;
                alu.DEC_R8(regs.H);
                break;

            // LD H, N
            case 0x26:
                tStates += 7;
                regs.H.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // DAA
            case 0x27:
                tStates += 4; {
                byte add = 0;
                boolean carry = regs.getCF();
                if (regs.getHF() || ((regs.A.getValue() & 0x0F) > 9)) {
                    add = 6;
                }
                if (carry || ((regs.A.getValue() & 0xFF) > 0x99)) {
                    add |= 0x60;
                }
                if ((regs.A.getValue() & 0xFF) > 0x99) {
                    carry = true;
                }
                if (regs.getNF()) {
                    alu.SUB_R8(add);
                } else {
                    alu.ADD_R8(add);
                }

                regs.setCF(carry);
                regs.setPF(parityTable[regs.A.getValue() & 0xFF]);
                regs.setF3((regs.A.getValue() & 0x08) != 0);
                regs.setF5((regs.A.getValue() & 0x20) != 0);

            }
                break;

            // JR Z, D
            case 0x28:
                tStates += 7;
                if (regs.getZF()) {
                    tStates += 5;
                    regs.setPC((short) (regs.getPC() + (byte) dataBus.memRead(regs.getPC() & 0xFFFF)));
                    regs.setWZ((short) ((regs.getPC() + 1) & 0xFFFF));
                }
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // ADD HL, HL
            case 0x29:
                tStates += 11;
                alu.ADD_R16(regs.HL, regs.HL.getValue16());
                break;

            // LD HL, (NN)
            case 0x2A:
                tStates += 16;
                regs.HL.setValue(read16(read16(regs.getPC())));
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // DEC HL
            case 0x2B:
                tStates += 6;
                regs.HL.setValue((short) (regs.HL.getValue16() - 1));
                break;

            // INC L
            case 0x2C:
                tStates += 4;
                alu.INC_R8(regs.L);
                break;

            // DEC L
            case 0x2D:
                tStates += 4;
                alu.DEC_R8(regs.L);
                break;

            // LD L, N
            case 0x2E:
                tStates += 7;
                regs.L.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // CPL
            case 0x2F:
                tStates += 4;
                regs.A.setValue((byte) (regs.A.getValue() ^ 0xFF));
                regs.setHF(true);
                regs.setNF(true);
                regs.setF3((regs.A.getValue() & 0x08) != 0);
                regs.setF5((regs.A.getValue() & 0x20) != 0);

                break;

            // jr nc,D
            case 0x30:
                tStates += 7;
                if (!regs.getCF()) {
                    tStates += 5;
                    regs.setPC((short) (regs.getPC() + (byte) dataBus.memRead(regs.getPC())));
                    regs.setWZ((short) ((regs.getPC() + 1) & 0xFFFF));
                }
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // ld sp,NN
            case 0x31:
                tStates += 10;
                regs.setSP(read16(regs.getPC()));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // ld (NN),a
            case 0x32:
                tStates += 13;
                regs.setWZ(read16(regs.getPC()));
                dataBus.memWrite(regs.getWZ(), (byte) regs.A.getValue());
                regs.setZ((byte) ((regs.getZ() + 1) & 0xFF));
                regs.setW(regs.A.getValue());
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // inc sp
            case 0x33:
                tStates += 6;
                regs.setSP((short) (regs.getSP() + 1));
                break;

            // inc (hl)
            case 0x34:
                tStates += 11; {
                Register regval = new Register(dataBus.memRead(regs.HL.getValue16()));
                alu.INC_R8(regval);
                dataBus.memWrite(regs.HL.getValue16(), (byte) regval.getValue());
            }
                break;

            // dec (hl)
            case 0x35:
                tStates += 11; {
                Register regval = new Register(dataBus.memRead(regs.HL.getValue16()));
                alu.DEC_R8(regval);
                dataBus.memWrite(regs.HL.getValue16(), (byte) regval.getValue());
            }
                break;

            // ld (hl),N
            case 0x36:
                tStates += 10;
                dataBus.memWrite(regs.HL.getValue16(), dataBus.memRead(regs.getPC()));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // scf
            case 0x37:
                tStates += 4;
                regs.setHF(false);
                regs.setNF(false);
                regs.setCF(true);
                if (regs.getLastQF()) {
                    regs.setF3(((regs.A.getValue() & 0x08) != 0));
                    regs.setF5(((regs.A.getValue() & 0x20) != 0));
                } else {
                    regs.setF3(((regs.A.getValue() & 0x08) != 0) || regs.getF3());
                    regs.setF5(((regs.A.getValue() & 0x20) != 0) || regs.getF5());
                }

                break;

            // jr c,D
            case 0x38:
                tStates += 7;
                if (regs.getCF()) {
                    tStates += 5;
                    regs.setPC((short) (regs.getPC() + (byte) dataBus.memRead(regs.getPC())));
                    regs.setWZ((short) ((regs.getPC() + 1) & 0xFFFF));
                }
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // add hl,sp
            case 0x39:
                tStates += 11;
                alu.ADD_R16(regs.HL, regs.getSP());
                break;

            // ld a,(NN)
            case 0x3A:
                tStates += 13;
                regs.A.setValue(dataBus.memRead(read16(regs.getPC())));
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // dec sp
            case 0x3B:
                tStates += 6;
                regs.setSP((short) (regs.getSP() - 1));
                break;

            // inc a
            case 0x3C:
                tStates += 4;
                alu.INC_R8(regs.A);
                break;

            // dec a
            case 0x3D:
                tStates += 4;
                alu.DEC_R8(regs.A);
                break;

            // ld a,N
            case 0x3E:
                tStates += 7;
                regs.A.setValue(dataBus.memRead(regs.getPC() & 0xFFFF));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // ccf
            case 0x3F:
                tStates += 4;
                regs.setHF(regs.getCF());
                regs.setNF(false);
                regs.setCF(!regs.getCF());
                if (regs.getLastQF()) {
                    regs.setF3(((regs.A.getValue() & 0x08) != 0));
                    regs.setF5(((regs.A.getValue() & 0x20) != 0));
                } else {
                    regs.setF3(((regs.A.getValue() & 0x08) != 0) || regs.getF3());
                    regs.setF5(((regs.A.getValue() & 0x20) != 0) || regs.getF5());
                }
                break;

            // ld b,b
            case 0x40:
                tStates += 4;
                break;

            // ld b,c
            case 0x41:
                tStates += 4;
                regs.B.setValue((byte) regs.C.getValue());
                break;

            // ld b,d
            case 0x42:
                tStates += 4;
                regs.B.setValue((byte) regs.D.getValue());
                break;

            // ld b,e
            case 0x43:
                tStates += 4;
                regs.B.setValue((byte) regs.E.getValue());
                break;

            // ld b,h
            case 0x44:
                tStates += 4;
                regs.B.setValue((byte) regs.H.getValue());
                break;

            // ld b,l
            case 0x45:
                tStates += 4;
                regs.B.setValue((byte) regs.L.getValue());
                break;

            // ld b,(hl)
            case 0x46:
                tStates += 7;
                regs.B.setValue(dataBus.memRead(regs.HL.getValue16() & 0xFFFF));
                break;

            // ld b,a
            case 0x47:
                tStates += 4;
                regs.B.setValue((byte) regs.A.getValue());
                break;

            // ld c,b
            case 0x48:
                tStates += 4;
                regs.C.setValue((byte) regs.B.getValue());
                break;

            // ld c,c
            case 0x49:
                tStates += 4;
                break;

            // ld c,d
            case 0x4A:
                tStates += 4;
                regs.C.setValue((byte) regs.D.getValue());
                break;

            // ld c,e
            case 0x4B:
                tStates += 4;
                regs.C.setValue((byte) regs.E.getValue());
                break;

            // ld c,h
            case 0x4C:
                tStates += 4;
                regs.C.setValue((byte) regs.H.getValue());
                break;

            // ld c,l
            case 0x4D:
                tStates += 4;
                regs.C.setValue((byte) regs.L.getValue());
                break;

            // ld c,(hl)
            case 0x4E:
                tStates += 7;
                regs.C.setValue(dataBus.memRead(regs.HL.getValue16() & 0xFFFF));
                break;

            // ld c,a
            case 0x4F:
                tStates += 4;
                regs.C.setValue((byte) regs.A.getValue());
                break;

            // ld d,b
            case 0x50:
                tStates += 4;
                regs.D.setValue((byte) regs.B.getValue());
                break;

            // ld d,c
            case 0x51:
                tStates += 4;
                regs.D.setValue((byte) regs.C.getValue());
                break;

            // ld d,d
            case 0x52:
                tStates += 4;
                break;

            // ld d,e
            case 0x53:
                tStates += 4;
                regs.D.setValue((byte) regs.E.getValue());
                break;

            // ld d,h
            case 0x54:
                tStates += 4;
                regs.D.setValue((byte) regs.H.getValue());
                break;

            // ld d,l
            case 0x55:
                tStates += 4;
                regs.D.setValue((byte) regs.L.getValue());
                break;

            // ld d,(hl)
            case 0x56:
                tStates += 7;
                regs.D.setValue(dataBus.memRead(regs.HL.getValue16()));
                break;

            // ld d,a
            case 0x57:
                tStates += 4;
                regs.D.setValue((byte) regs.A.getValue());
                break;

            // ld e,b
            case 0x58:
                tStates += 4;
                regs.E.setValue((byte) regs.B.getValue());
                break;

            // ld e,c
            case 0x59:
                tStates += 4;
                regs.E.setValue((byte) regs.C.getValue());
                break;

            // ld e,d
            case 0x5A:
                tStates += 4;
                regs.E.setValue((byte) regs.D.getValue());
                break;

            // ld e,e
            case 0x5B:
                tStates += 4;
                break;

            // ld e,h
            case 0x5C:
                tStates += 4;
                regs.E.setValue((byte) regs.H.getValue());
                break;

            // ld e,l
            case 0x5D:
                tStates += 4;
                regs.E.setValue((byte) regs.L.getValue());
                break;

            // ld e,(hl)
            case 0x5E:
                tStates += 7;
                regs.E.setValue(dataBus.memRead(regs.HL.getValue16()));
                break;

            // ld e,a
            case 0x5F:
                tStates += 4;
                regs.E.setValue((byte) regs.A.getValue());
                break;

            // ld h,b
            case 0x60:
                tStates += 4;
                regs.H.setValue((byte) regs.B.getValue());
                break;

            // ld h,c
            case 0x61:
                tStates += 4;
                regs.H.setValue((byte) regs.C.getValue());
                break;

            // ld h,d
            case 0x62:
                tStates += 4;
                regs.H.setValue((byte) regs.D.getValue());
                break;

            // ld h,e
            case 0x63:
                tStates += 4;
                regs.H.setValue((byte) regs.E.getValue());
                break;

            // ld h,h
            case 0x64:
                tStates += 4;
                break;

            // ld h,l
            case 0x65:
                tStates += 4;
                regs.H.setValue((byte) regs.L.getValue());
                break;

            // ld h,(hl)
            case 0x66:
                tStates += 7;
                regs.H.setValue(dataBus.memRead(regs.HL.getValue16()));
                break;

            // ld h,a
            case 0x67:
                tStates += 4;
                regs.H.setValue((byte) regs.A.getValue());
                break;

            // ld l,b
            case 0x68:
                tStates += 4;
                regs.L.setValue((byte) regs.B.getValue());
                break;

            // ld l,c
            case 0x69:
                tStates += 4;
                regs.L.setValue((byte) regs.C.getValue());
                break;

            // ld l,d
            case 0x6A:
                tStates += 4;
                regs.L.setValue((byte) regs.D.getValue());
                break;

            // ld l,e
            case 0x6B:
                tStates += 4;
                regs.L.setValue((byte) regs.E.getValue());
                break;

            // ld l,h
            case 0x6C:
                tStates += 4;
                regs.L.setValue((byte) regs.H.getValue());
                break;

            // ld l,l
            case 0x6D:
                tStates += 4;
                break;

            // ld l,(hl)
            case 0x6E:
                tStates += 7;
                regs.L.setValue(dataBus.memRead(regs.HL.getValue16()));
                break;

            // ld l,a
            case 0x6F:
                tStates += 4;
                regs.L.setValue((byte) regs.A.getValue());
                break;

            // ld (hl),b
            case 0x70:
                tStates += 7;
                dataBus.memWrite(regs.HL.getValue16(), (byte) regs.B.getValue());
                break;

            // ld (hl),c
            case 0x71:
                tStates += 7;
                dataBus.memWrite(regs.HL.getValue16(), (byte) regs.C.getValue());
                break;

            // ld (hl),d
            case 0x72:
                tStates += 7;
                dataBus.memWrite(regs.HL.getValue16(), (byte) regs.D.getValue());
                break;

            // ld (hl),e
            case 0x73:
                tStates += 7;
                dataBus.memWrite(regs.HL.getValue16(), (byte) regs.E.getValue());
                break;

            // ld (hl),h
            case 0x74:
                tStates += 7;
                dataBus.memWrite(regs.HL.getValue16(), (byte) regs.H.getValue());
                break;

            // ld (hl),l
            case 0x75:
                tStates += 7;
                dataBus.memWrite(regs.HL.getValue16(), (byte) regs.L.getValue());
                break;

            // halt
            case 0x76:
                tStates += 4;
                regs.setPC((short) (regs.getPC() - 1)); // Keep executing the same instruction
                break;

            // ld (hl),a
            case 0x77:
                tStates += 7;
                dataBus.memWrite(regs.HL.getValue16(), (byte) regs.A.getValue());
                break;

            // ld a,b
            case 0x78:
                tStates += 4;
                regs.A.setValue((byte) regs.B.getValue());
                break;

            // ld a,c
            case 0x79:
                tStates += 4;
                regs.A.setValue((byte) regs.C.getValue());
                break;

            // ld a,d
            case 0x7A:
                tStates += 4;
                regs.A.setValue((byte) regs.D.getValue());
                break;

            // ld a,e
            case 0x7B:
                tStates += 4;
                regs.A.setValue((byte) regs.E.getValue());
                break;

            // ld a,h
            case 0x7C:
                tStates += 4;
                regs.A.setValue((byte) regs.H.getValue());
                break;

            // ld a,l
            case 0x7D:
                tStates += 4;
                regs.A.setValue((byte) regs.L.getValue());
                break;

            // ld a,(hl)
            case 0x7E:
                tStates += 7;
                regs.A.setValue(dataBus.memRead(regs.HL.getValue16()));
                break;

            // ld a,a
            case 0x7F:
                tStates += 4;
                break;

            // add a,b
            case 0x80:
                tStates += 4;
                alu.ADD_R8((byte) regs.B.getValue());
                break;

            // add a,c
            case 0x81:
                tStates += 4;
                alu.ADD_R8((byte) regs.C.getValue());
                break;

            // add a,d
            case 0x82:
                tStates += 4;
                alu.ADD_R8((byte) regs.D.getValue());
                break;

            // add a,e
            case 0x83:
                tStates += 4;
                alu.ADD_R8((byte) regs.E.getValue());
                break;

            // add a,h
            case 0x84:
                tStates += 4;
                alu.ADD_R8((byte) regs.H.getValue());
                break;

            // add a,l
            case 0x85:
                tStates += 4;
                alu.ADD_R8((byte) regs.L.getValue());
                break;

            // add a,(hl)
            case 0x86:
                tStates += 7; {
                int b = dataBus.memRead(regs.HL.getValue16());
                alu.ADD_R8((byte) b);
            }
                break;

            // add a,a
            case 0x87:
                tStates += 4;
                alu.ADD_R8((byte) regs.A.getValue());
                break;

            // adc a,b
            case 0x88:
                tStates += 4;
                alu.ADC_R8((byte) regs.B.getValue());
                break;

            // adc a,c
            case 0x89:
                tStates += 4;
                alu.ADC_R8((byte) regs.C.getValue());
                break;

            // adc a,d
            case 0x8A:
                tStates += 4;
                alu.ADC_R8((byte) regs.D.getValue());
                break;

            // adc a,e
            case 0x8B:
                tStates += 4;
                alu.ADC_R8((byte) regs.E.getValue());
                break;

            // adc a,h
            case 0x8C:
                tStates += 4;
                alu.ADC_R8((byte) regs.H.getValue());
                break;

            // adc a,l
            case 0x8D:
                tStates += 4;
                alu.ADC_R8((byte) regs.L.getValue());
                break;

            // adc a,(hl)
            case 0x8E:
                tStates += 7; {
                int val = dataBus.memRead(regs.HL.getValue16());
                alu.ADC_R8((byte) val);
            }
                break;

            // adc a,a
            case 0x8F:
                tStates += 4;
                alu.ADC_R8((byte) regs.A.getValue());
                break;

            // sub b
            case 0x90:
                tStates += 4;
                alu.SUB_R8((byte) regs.B.getValue());
                break;

            // sub c
            case 0x91:
                tStates += 4;
                alu.SUB_R8((byte) regs.C.getValue());
                break;

            // sub d
            case 0x92:
                tStates += 4;
                alu.SUB_R8((byte) regs.D.getValue());
                break;

            // sub e
            case 0x93:
                tStates += 4;
                alu.SUB_R8((byte) regs.E.getValue());
                break;

            // sub h
            case 0x94:
                tStates += 4;
                alu.SUB_R8((byte) regs.H.getValue());
                break;

            // sub l
            case 0x95:
                tStates += 4;
                alu.SUB_R8((byte) regs.L.getValue());
                break;

            // sub (hl)
            case 0x96:
                tStates += 7; {
                int val = dataBus.memRead(regs.HL.getValue16());
                alu.SUB_R8((byte) val);
            }
                break;

            // sub a
            case 0x97:
                tStates += 4;
                alu.SUB_R8((byte) regs.A.getValue());
                break;

            // sbc a,b
            case 0x98:
                tStates += 4;
                alu.SBC_R8((byte) regs.B.getValue());
                break;

            // sbc a,c
            case 0x99:
                tStates += 4;
                alu.SBC_R8((byte) regs.C.getValue());
                break;

            // sbc a,d
            case 0x9A:
                tStates += 4;
                alu.SBC_R8((byte) regs.D.getValue());
                break;

            // sbc a,e
            case 0x9B:
                tStates += 4;
                alu.SBC_R8((byte) regs.E.getValue());
                break;

            // sbc a,h
            case 0x9C:
                tStates += 4;
                alu.SBC_R8((byte) regs.H.getValue());
                break;

            // sbc a,l
            case 0x9D:
                tStates += 4;
                alu.SBC_R8((byte) regs.L.getValue());
                break;

            // sbc a,(hl)
            case 0x9E:
                tStates += 7; {
                int val = dataBus.memRead(regs.HL.getValue16());
                alu.SBC_R8((byte) val);
            }
                break;

            // sbc a,a
            case 0x9F:
                tStates += 4;
                alu.SBC_R8((byte) regs.A.getValue());
                break;

            // and b
            case 0xA0:
                tStates += 4;
                alu.AND_R8((byte) regs.B.getValue());
                break;

            // and c
            case 0xA1:
                tStates += 4;
                alu.AND_R8((byte) regs.C.getValue());
                break;

            // and d
            case 0xA2:
                tStates += 4;
                alu.AND_R8((byte) regs.D.getValue());
                break;

            // and e
            case 0xA3:
                tStates += 4;
                alu.AND_R8((byte) regs.E.getValue());
                break;

            // and h
            case 0xA4:
                tStates += 4;
                alu.AND_R8((byte) regs.H.getValue());
                break;

            // and l
            case 0xA5:
                tStates += 4;
                alu.AND_R8((byte) regs.L.getValue());
                break;

            // and (hl)
            case 0xA6:
                tStates += 7; {
                int val = dataBus.memRead(regs.HL.getValue16());
                alu.AND_R8((byte) val);
            }
                break;

            // and a
            case 0xA7:
                tStates += 4;
                alu.AND_R8((byte) regs.A.getValue());
                break;

            // xor b
            case 0xA8:
                tStates += 4;
                alu.XOR_R8((byte) regs.B.getValue());
                break;

            // xor c
            case 0xA9:
                tStates += 4;
                alu.XOR_R8((byte) regs.C.getValue());
                break;

            // xor d
            case 0xAA:
                tStates += 4;
                alu.XOR_R8((byte) regs.D.getValue());
                break;

            // xor e
            case 0xAB:
                tStates += 4;
                alu.XOR_R8((byte) regs.E.getValue());
                break;

            // xor h
            case 0xAC:
                tStates += 4;
                alu.XOR_R8((byte) regs.H.getValue());
                break;

            // xor l
            case 0xAD:
                tStates += 4;
                alu.XOR_R8((byte) regs.L.getValue());
                break;

            // xor (hl)
            case 0xAE:
                tStates += 7; {
                int val = dataBus.memRead(regs.HL.getValue16());
                alu.XOR_R8((byte) val);
            }
                break;

            // xor a
            case 0xAF:
                tStates += 4;
                alu.XOR_R8((byte) regs.A.getValue());
                break;

            // or b
            case 0xB0:
                tStates += 4;
                alu.OR_R8((byte) regs.B.getValue());
                break;

            // or c
            case 0xB1:
                tStates += 4;
                alu.OR_R8((byte) regs.C.getValue());
                break;

            // or d
            case 0xB2:
                tStates += 4;
                alu.OR_R8((byte) regs.D.getValue());
                break;

            // or e
            case 0xB3:
                tStates += 4;
                alu.OR_R8((byte) regs.E.getValue());
                break;

            // or h
            case 0xB4:
                tStates += 4;
                alu.OR_R8((byte) regs.H.getValue());
                break;

            // or l
            case 0xB5:
                tStates += 4;
                alu.OR_R8((byte) regs.L.getValue());
                break;

            // or (hl)
            case 0xB6:
                tStates += 7; {
                int val = dataBus.memRead(regs.HL.getValue16());
                alu.OR_R8((byte) val);
            }
                break;

            // or a
            case 0xB7:
                tStates += 4;
                alu.OR_R8((byte) regs.A.getValue());
                break;

            // cp b
            case 0xB8:
                tStates += 4;
                alu.CMP_R8((byte) regs.B.getValue());
                break;

            // cp c
            case 0xB9:
                tStates += 4;
                alu.CMP_R8((byte) regs.C.getValue());
                break;

            // cp d
            case 0xBA:
                tStates += 4;
                alu.CMP_R8((byte) regs.D.getValue());
                break;

            // cp e
            case 0xBB:
                tStates += 4;
                alu.CMP_R8((byte) regs.E.getValue());
                break;

            // cp h
            case 0xBC:
                tStates += 4;
                alu.CMP_R8((byte) regs.H.getValue());
                break;

            // cp l
            case 0xBD:
                tStates += 4;
                alu.CMP_R8((byte) regs.L.getValue());
                break;

            // cp (hl)
            case 0xBE:
                tStates += 7; {
                int val = dataBus.memRead(regs.HL.getValue16());
                alu.CMP_R8((byte) val);
            }
                break;

            // cp a
            case 0xBF:
                tStates += 4;
                alu.CMP_R8((byte) regs.A.getValue());
                break;

            // ret nz
            case 0xC0:
                tStates += 5;
                if (!regs.getZF()) {
                    tStates += 6;
                    regs.setPC((short) pop16());
                    regs.setWZ(regs.getPC());
                }
                break;

            // pop bc
            case 0xC1:
                tStates += 10;
                regs.BC.setValue(pop16());
                break;

            // jp nz,NN
            case 0xC2:
                tStates += 10;
                regs.setWZ(read16(regs.getPC()));
                if (!regs.getZF()) {
                    regs.setPC((short) read16(regs.getPC()));
                } else {
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // jp NN
            case 0xC3:
                tStates += 10;
                regs.setPC((short) read16(regs.getPC()));
                regs.setWZ(regs.getPC());
                break;

            // call nz,NN
            case 0xC4:
                regs.setWZ((short) read16(regs.getPC()));
                if (!regs.getZF()) {
                    tStates += 17;
                    push16((short) (regs.getPC() + 2));
                    regs.setPC((short) read16(regs.getPC()));
                } else {
                    tStates += 10;
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // push bc
            case 0xC5:
                tStates += 11;
                push16(regs.BC.getValue16());
                break;

            // add a,N
            case 0xC6:
                tStates += 7; {
                int val = dataBus.memRead(regs.getPC());
                regs.setPC((short) (regs.getPC() + 1));
                alu.ADD_R8((byte) val);
            }
                break;

            // rst 0x00
            case 0xC7:
                tStates += 11;
                push16(regs.getPC());
                regs.setPC((short) 0x0000);
                regs.setWZ((short) 0x0000);
                break;

            // ret z
            case 0xC8:
                tStates += 5;
                if (regs.getZF()) {
                    tStates += 6;
                    regs.setPC((short) pop16());
                    regs.setWZ(regs.getPC());
                }
                break;

            // ret
            case 0xC9:
                tStates += 10;
                regs.setPC((short) pop16());
                regs.setWZ(regs.getPC());
                break;

            // jp z,NN
            case 0xCA:
                tStates += 10;
                regs.setWZ(read16(regs.getPC()));
                if (regs.getZF()) {
                    regs.setPC(read16(regs.getPC()));
                } else {
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // 0xCB instructions prefix
            case 0xCB:
                execInstCB();
                break;

            // call z,NN
            case 0xCC:
                regs.setWZ((short) read16(regs.getPC()));
                if (regs.getZF()) {
                    tStates += 17;
                    push16((short) (regs.getPC() + 2));
                    regs.setPC(read16(regs.getPC()));
                } else {
                    tStates += 10;
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // call NN
            case 0xCD:
                tStates += 17;
                regs.setWZ((short) read16(regs.getPC()));
                push16((short) (regs.getPC() + 2));
                regs.setPC(read16(regs.getPC()));
                break;

            // adc a,N
            case 0xCE:
                tStates += 7; {
                int val = dataBus.memRead(regs.getPC());
                regs.setPC((short) (regs.getPC() + 1));
                alu.ADC_R8((byte) val);
            }
                break;

            // rst 0x08
            case 0xCF:
                tStates += 11;
                push16(regs.getPC());
                regs.setPC((short) 0x0008);
                regs.setWZ((short) 0x0008);
                break;

            // ret nc
            case 0xD0:
                tStates += 5;
                if (!regs.getCF()) {
                    tStates += 6;
                    regs.setPC((short) pop16());
                    regs.setWZ(regs.getPC());
                }
                break;

            // pop de
            case 0xD1:
                tStates += 10;
                regs.DE.setValue(pop16());
                break;

            // jp nc,NN
            case 0xD2:
                tStates += 10;
                regs.setWZ(read16(regs.getPC()));
                if (!regs.getCF()) {
                    regs.setPC(read16(regs.getPC()));
                } else {
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // out (N),a
            case 0xD3:
                tStates += 11;
                dataBus.ioWrite(dataBus.memRead(regs.getPC()) | (regs.A.getValue() << 8), (byte) regs.A.getValue());
                regs.setZ((byte) ((dataBus.memRead(regs.getPC()) + 1) & 0xFF));
                regs.setW(regs.A.getValue());
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // call nc,NN
            case 0xD4:
                regs.setWZ((short) read16(regs.getPC()));
                if (!regs.getCF()) {
                    tStates += 17;
                    push16((short) (regs.getPC() + 2));
                    regs.setPC(read16(regs.getPC()));
                } else {
                    tStates += 10;
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // push de
            case 0xD5:
                tStates += 11;
                push16(regs.DE.getValue16());
                break;

            // sub N
            case 0xD6:
                tStates += 7; {
                int val = dataBus.memRead(regs.getPC());
                regs.setPC((short) (regs.getPC() + 1));
                alu.SUB_R8((byte) val);
            }
                break;

            // rst 0x10
            case 0xD7:
                tStates += 11;
                push16(regs.getPC());
                regs.setPC((short) 0x0010);
                regs.setWZ((short) 0x0010);
                break;

            // ret c
            case 0xD8:
                tStates += 5;
                if (regs.getCF()) {
                    tStates += 6;
                    regs.setPC((short) pop16());
                    regs.setWZ(regs.getPC());
                }
                break;

            // exx
            case 0xD9:
                tStates += 4; {
                int aux;
                aux = regs.BC.getValue16();
                regs.BC.setValue(regs.altBC.getValue16());
                regs.altBC.setValue((short) aux);

                aux = regs.DE.getValue16();
                regs.DE.setValue(regs.altDE.getValue16());
                regs.altDE.setValue((short) aux);

                aux = regs.HL.getValue16();
                regs.HL.setValue(regs.altHL.getValue16());
                regs.altHL.setValue((short) aux);
            }
                break;

            // jp c,NN
            case 0xDA:
                tStates += 10;
                regs.setWZ(read16(regs.getPC()));
                if (regs.getCF()) {
                    regs.setPC(read16(regs.getPC()));
                } else {
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // in a,(N)
            case 0xDB:
                tStates += 11;
                regs.setW((byte) regs.A.getValue());
                regs.setZ((byte) dataBus.memRead(regs.getPC()));
                regs.A.setValue(dataBus.ioRead(regs.getWZ()));
                regs.setPC((short) (regs.getPC() + 1));
                regs.setWZ((short) (regs.getWZ() + 1));
                break;

            // call c,NN
            case 0xDC:
                regs.setWZ((short) read16(regs.getPC()));
                if (regs.getCF()) {
                    tStates += 17;
                    push16((short) (regs.getPC() + 2));
                    regs.setPC(read16(regs.getPC()));
                } else {
                    tStates += 10;
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // IX register operations
            case 0xDD:
                regs.XX.setValue(regs.IX.getValue16());
                execInstXX();
                regs.IX.setValue(regs.XX.getValue16());
                break;

            // sbc a,N
            case 0xDE:
                tStates += 7; {
                int val = dataBus.memRead(regs.getPC());
                regs.setPC((short) (regs.getPC() + 1));
                alu.SBC_R8((byte) val);
            }
                break;

            // rst 0x18
            case 0xDF:
                tStates += 11;
                push16(regs.getPC());
                regs.setPC((short) 0x0018);
                regs.setWZ((short) 0x0018);
                break;

            // ret po
            case 0xE0:
                tStates += 5;
                if (!regs.getPF()) {
                    tStates += 6;
                    regs.setPC((short) pop16());
                    regs.setWZ(regs.getPC());
                }
                break;

            // pop hl
            case 0xE1:
                tStates += 10;
                regs.HL.setValue(pop16());
                break;

            // jp po,NN
            case 0xE2:
                tStates += 10;
                regs.setWZ(read16(regs.getPC()));
                if (!regs.getPF()) {
                    regs.setPC(read16(regs.getPC()));
                } else {
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // ex (sp),hl
            case 0xE3:
                tStates += 19; {
                int val = read16(regs.getSP());
                write16(regs.getSP(), regs.HL.getValue16());
                regs.HL.setValue((short) val);
                regs.setWZ((short) val);
            }
                break;

            // call po,NN
            case 0xE4:
                regs.setWZ((short) read16(regs.getPC()));
                if (!regs.getPF()) {
                    tStates += 17;
                    push16((short) (regs.getPC() + 2));
                    regs.setPC(read16(regs.getPC()));
                } else {
                    tStates += 10;
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // push hl
            case 0xE5:
                tStates += 11;
                push16(regs.HL.getValue16());
                break;

            // and N
            case 0xE6:
                tStates += 7; {
                int val = dataBus.memRead(regs.getPC());
                regs.setPC((short) (regs.getPC() + 1));
                alu.AND_R8((byte) val);
            }
                break;

            // rst 0x20
            case 0xE7:
                tStates += 11;
                push16(regs.getPC());
                regs.setPC((short) 0x0020);
                regs.setWZ((short) 0x0020);
                break;

            // ret pe
            case 0xE8:
                tStates += 5;
                if (regs.getPF()) {
                    tStates += 6;
                    regs.setPC((short) pop16());
                    regs.setWZ(regs.getPC());
                }
                break;

            // jp (hl)
            case 0xE9:
                tStates += 4;
                regs.setPC(regs.HL.getValue16());
                break;

            // jp pe,NN
            case 0xEA:
                tStates += 10;
                regs.setWZ(read16(regs.getPC()));
                if (regs.getPF()) {
                    regs.setPC(read16(regs.getPC()));
                } else {
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // ex de,hl
            case 0xEB:
                tStates += 4; {
                int aux = regs.DE.getValue16();
                regs.DE.setValue(regs.HL.getValue16());
                regs.HL.setValue((short) aux);
            }
                break;

            // call pe,NN
            case 0xEC:
                regs.setWZ((short) read16(regs.getPC()));
                if (regs.getPF()) {
                    tStates += 17;
                    push16((short) (regs.getPC() + 2));
                    regs.setPC(read16(regs.getPC()));
                } else {
                    tStates += 10;
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // 0xED instructions prefix
            case 0xED:
                execInstED();
                break;

            // xor N
            case 0xEE:
                tStates += 7; {
                int val = dataBus.memRead(regs.getPC());
                regs.setPC((short) (regs.getPC() + 1));
                alu.XOR_R8((byte) val);
            }
                break;

            // rst 0x28
            case 0xEF:
                tStates += 11;
                push16(regs.getPC());
                regs.setPC((short) 0x0028);
                regs.setWZ((short) 0x0028);
                break;

            // ret p
            case 0xF0:
                tStates += 5;
                if (!regs.getSF()) {
                    tStates += 6;
                    regs.setPC((short) pop16());
                    regs.setWZ(regs.getPC());
                }
                break;

            // pop af
            case 0xF1:
                tStates += 10;
                regs.setAF(pop16());
                break;

            // jp p,NN
            case 0xF2:
                tStates += 10;
                regs.setWZ(read16(regs.getPC()));
                if (!regs.getSF()) {
                    regs.setPC(read16(regs.getPC()));
                } else {
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // di
            case 0xF3:
                tStates += 4;
                regs.setiff1A(false);
                regs.setiff1B(false);
                break;

            // call p,NN
            case 0xF4:
                regs.setWZ((short) read16(regs.getPC()));
                if (!regs.getSF()) {
                    tStates += 17;
                    push16((short) (regs.getPC() + 2));
                    regs.setPC(read16(regs.getPC()));
                } else {
                    tStates += 10;
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // push af
            case 0xF5:
                tStates += 11;
                push16(regs.getAF());
                break;

            // or N
            case 0xF6:
                tStates += 7; {
                int val = dataBus.memRead(regs.getPC());
                regs.setPC((short) (regs.getPC() + 1));
                alu.OR_R8((byte) val);
            }
                break;

            // rst 0x30
            case 0xF7:
                tStates += 11;
                push16(regs.getPC());
                regs.setPC((short) 0x0030);
                regs.setWZ((short) 0x0030);
                break;

            // ret m
            case 0xF8:
                tStates += 5;
                if (regs.getSF()) {
                    tStates += 6;
                    regs.setPC((short) pop16());
                    regs.setWZ(regs.getPC());
                }
                break;

            // ld sp,hl
            case 0xF9:
                tStates += 6;
                regs.setSP(regs.HL.getValue16());
                break;

            // jp m,NN
            case 0xFA:
                tStates += 10;
                regs.setWZ(read16(regs.getPC()));
                if (regs.getSF()) {
                    regs.setPC(read16(regs.getPC()));
                } else {
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // ei
            case 0xFB:
                tStates += 4;
                regs.setiff1A(true);
                regs.setiff1B(true);
                break;

            // call m,NN
            case 0xFC:
                regs.setWZ((short) read16(regs.getPC()));
                if (regs.getSF()) {
                    tStates += 17;
                    push16((short) (regs.getPC() + 2));
                    regs.setPC(read16(regs.getPC()));
                } else {
                    tStates += 10;
                    regs.setPC((short) (regs.getPC() + 2));
                }
                break;

            // IY register operation prefix
            case 0xFD:
                regs.XX.setValue(regs.IY.getValue16());
                execInstXX();
                regs.IY.setValue(regs.XX.getValue16());
                break;

            // cp N
            case 0xFE:
                tStates += 7; {

                byte val = dataBus.memRead(regs.getPC());
                regs.setPC((short) (regs.getPC() + 1));
                alu.CMP_R8(val);
            }
                break;

            // rst 0x38
            case 0xFF:
                tStates += 11;
                push16(regs.getPC());
                regs.setPC((short) 0x0038);
                regs.setWZ((short) 0x0038);
                break;

        }

        // Salvamos QF
        regs.preserveQF();

    }

    /////////////////////////////////////////////////////////////////////////
    // execInstCB():
    // Ejecuta una instrucción CB
    public void execInstCB() {

        // Obtener la siguiente instrucción (fetch)
        byte op = dataBus.memReadOpCode(regs.getPC() & 0xFFFF);
        regs.setPC((short) (regs.getPC() + 1)); // Incrementar el contador de programa

        // Increment instruction counter register
        REFRESH_CYCLE();

        switch (op & 0xFF) {

            // Instrucciones CB

            // rlc b
            case 0x00:
                tStates += 8;
                alu.RLC8(regs.B);
                break;

            // rlc c
            case 0x01:
                tStates += 8;
                alu.RLC8(regs.C);
                break;

            // rlc d
            case 0x02:
                tStates += 8;
                alu.RLC8(regs.D);
                break;

            // rlc e
            case 0x03:
                tStates += 8;
                alu.RLC8(regs.E);
                break;

            // rlc h
            case 0x04:
                tStates += 8;
                alu.RLC8(regs.H);
                break;

            // rlc l
            case 0x05:
                tStates += 8;
                alu.RLC8(regs.L);
                break;

            // rlc (hl)
            case 0x06:
                tStates += 15; {
                Register regval = new Register(dataBus.memRead(regs.HL.getValue16()));
                alu.RLC8(regval);
                dataBus.memWrite(regs.HL.getValue16(), (byte) regval.getValue());
            }
                break;

            // rlc a
            case 0x07:
                tStates += 8;
                alu.RLC8(regs.A);
                break;

            // rrc b
            case 0x08:
                tStates += 8;
                alu.RRC8(regs.B);
                break;

            // rrc c
            case 0x09:
                tStates += 8;
                alu.RRC8(regs.C);
                break;

            // rrc d
            case 0x0A:
                tStates += 8;
                alu.RRC8(regs.D);
                break;

            // rrc e
            case 0x0B:
                tStates += 8;
                alu.RRC8(regs.E);
                break;

            // rrc h
            case 0x0C:
                tStates += 8;
                alu.RRC8(regs.H);
                break;

            // rrc l
            case 0x0D:
                tStates += 8;
                alu.RRC8(regs.L);
                break;

            // rrc (hl)
            case 0x0E:
                tStates += 15; {
                Register regval = new Register(dataBus.memRead(regs.HL.getValue16()));
                alu.RRC8(regval);
                dataBus.memWrite(regs.HL.getValue16(), (byte) regval.getValue());
            }
                break;

            // rrc a
            case 0x0F:
                tStates += 8;
                alu.RRC8(regs.A);
                break;

            // rl b
            case 0x10:
                tStates += 8;
                alu.RL8(regs.B);
                break;

            // rl c
            case 0x11:
                tStates += 8;
                alu.RL8(regs.C);
                break;

            // rl d
            case 0x12:
                tStates += 8;
                alu.RL8(regs.D);
                break;

            // rl e
            case 0x13:
                tStates += 8;
                alu.RL8(regs.E);
                break;

            // rl h
            case 0x14:
                tStates += 8;
                alu.RL8(regs.H);
                break;

            // rl l
            case 0x15:
                tStates += 8;
                alu.RL8(regs.L);
                break;

            // rl (hl)
            case 0x16:
                tStates += 15; {
                Register regval = new Register(dataBus.memRead(regs.HL.getValue16()));
                alu.RL8(regval);
                dataBus.memWrite(regs.HL.getValue16(), (byte) regval.getValue());
            }
                break;

            // rl a
            case 0x17:
                tStates += 8;
                alu.RL8(regs.A);
                break;

            // rr b
            case 0x18:
                tStates += 8;
                alu.RR8(regs.B);
                break;

            // rr c
            case 0x19:
                tStates += 8;
                alu.RR8(regs.C);
                break;

            // rr d
            case 0x1A:
                tStates += 8;
                alu.RR8(regs.D);
                break;

            // rr e
            case 0x1B:
                tStates += 8;
                alu.RR8(regs.E);
                break;

            // rr h
            case 0x1C:
                tStates += 8;
                alu.RR8(regs.H);
                break;

            // rr l
            case 0x1D:
                tStates += 8;
                alu.RR8(regs.L);
                break;

            // rr (hl)
            case 0x1E:
                tStates += 15; {
                Register regval = new Register(dataBus.memRead(regs.HL.getValue16()));
                alu.RR8(regval);
                dataBus.memWrite(regs.HL.getValue16(), (byte) regval.getValue());
            }
                break;

            // rr a
            case 0x1F:
                tStates += 8;
                alu.RR8(regs.A);
                break;

            // sla b
            case 0x20:
                tStates += 8;
                alu.SLA8(regs.B);
                break;

            // sla c
            case 0x21:
                tStates += 8;
                alu.SLA8(regs.C);
                break;

            // sla d
            case 0x22:
                tStates += 8;
                alu.SLA8(regs.D);
                break;

            // sla e
            case 0x23:
                tStates += 8;
                alu.SLA8(regs.E);
                break;

            // sla h
            case 0x24:
                tStates += 8;
                alu.SLA8(regs.H);
                break;

            // sla l
            case 0x25:
                tStates += 8;
                alu.SLA8(regs.L);
                break;

            // sla (hl)
            case 0x26:
                tStates += 15; {
                Register regval = new Register(dataBus.memRead(regs.HL.getValue16()));
                alu.SLA8(regval);
                dataBus.memWrite(regs.HL.getValue16(), (byte) regval.getValue());
            }
                break;

            // sla a
            case 0x27:
                tStates += 8;
                alu.SLA8(regs.A);
                break;

            // sra b
            case 0x28:
                tStates += 8;
                alu.SRA8(regs.B);
                break;

            // sra c
            case 0x29:
                tStates += 8;
                alu.SRA8(regs.C);
                break;

            // sra d
            case 0x2A:
                tStates += 8;
                alu.SRA8(regs.D);
                break;

            // sra e
            case 0x2B:
                tStates += 8;
                alu.SRA8(regs.E);
                break;

            // sra h
            case 0x2C:
                tStates += 8;
                alu.SRA8(regs.H);
                break;

            // sra l
            case 0x2D:
                tStates += 8;
                alu.SRA8(regs.L);
                break;

            // sra (hl)
            case 0x2E:
                tStates += 15; {
                Register regval = new Register(dataBus.memRead(regs.HL.getValue16()));
                alu.SRA8(regval);
                dataBus.memWrite(regs.HL.getValue16(), (byte) regval.getValue());
            }
                break;

            // sra a
            case 0x2F:
                tStates += 8;
                alu.SRA8(regs.A);
                break;

            // sli b
            case 0x30:
                tStates += 8;
                alu.SLI8(regs.B);
                break;

            // sli c
            case 0x31:
                tStates += 8;
                alu.SLI8(regs.C);
                break;

            // sli d
            case 0x32:
                tStates += 8;
                alu.SLI8(regs.D);
                break;

            // sli e
            case 0x33:
                tStates += 8;
                alu.SLI8(regs.E);
                break;

            // sli h
            case 0x34:
                tStates += 8;
                alu.SLI8(regs.H);
                break;

            // sli l
            case 0x35:
                tStates += 8;
                alu.SLI8(regs.L);
                break;

            // sli (hl)
            case 0x36:
                tStates += 15; {
                Register regval = new Register(dataBus.memRead(regs.HL.getValue16()));
                alu.SLI8(regval);
                dataBus.memWrite(regs.HL.getValue16(), (byte) regval.getValue());
            }
                break;

            // sli a
            case 0x37:
                tStates += 8;
                alu.SLI8(regs.A);
                break;

            // srl b
            case 0x38:
                tStates += 8;
                alu.SRL8(regs.B);
                break;

            // srl c
            case 0x39:
                tStates += 8;
                alu.SRL8(regs.C);
                break;

            // srl d
            case 0x3A:
                tStates += 8;
                alu.SRL8(regs.D);
                break;

            // srl e
            case 0x3B:
                tStates += 8;
                alu.SRL8(regs.E);
                break;

            // srl h
            case 0x3C:
                tStates += 8;
                alu.SRL8(regs.H);
                break;

            // srl l
            case 0x3D:
                tStates += 8;
                alu.SRL8(regs.L);
                break;

            // srl (hl)
            case 0x3E:
                tStates += 15; {
                Register regval = new Register(dataBus.memRead(regs.HL.getValue16()));
                alu.SRL8(regval);
                dataBus.memWrite(regs.HL.getValue16(), (byte) regval.getValue());
            }
                break;

            // srl a
            case 0x3F:
                tStates += 8;
                alu.SRL8(regs.A);
                break;

            // bit 0,b
            case 0x40:
                tStates += 8;
                alu.BIT8((byte) regs.B.getValue(), 0);
                break;

            // bit 0,c
            case 0x41:
                tStates += 8;
                alu.BIT8((byte) regs.C.getValue(), 0);
                break;

            // bit 0,d
            case 0x42:
                tStates += 8;
                alu.BIT8((byte) regs.D.getValue(), 0);
                break;

            // bit 0,e
            case 0x43:
                tStates += 8;
                alu.BIT8((byte) regs.E.getValue(), 0);
                break;

            // bit 0,h
            case 0x44:
                tStates += 8;
                alu.BIT8((byte) regs.H.getValue(), 0);
                break;

            // bit 0,l
            case 0x45:
                tStates += 8;
                alu.BIT8((byte) regs.L.getValue(), 0);
                break;

            // bit 0,(hl)
            case 0x46:
                tStates += 12; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                alu.BIT8_WZ_(val, 0);
            }
                break;

            // bit 0,a
            case 0x47:
                tStates += 8;
                alu.BIT8((byte) regs.A.getValue(), 0);
                break;

            // bit 1,b
            case 0x48:
                tStates += 8;
                alu.BIT8((byte) regs.B.getValue(), 1);
                break;

            // bit 1,c
            case 0x49:
                tStates += 8;
                alu.BIT8((byte) regs.C.getValue(), 1);
                break;

            // bit 1,d
            case 0x4A:
                tStates += 8;
                alu.BIT8((byte) regs.D.getValue(), 1);
                break;

            // bit 1,e
            case 0x4B:
                tStates += 8;
                alu.BIT8((byte) regs.E.getValue(), 1);
                break;

            // bit 1,h
            case 0x4C:
                tStates += 8;
                alu.BIT8((byte) regs.H.getValue(), 1);
                break;

            // bit 1,l
            case 0x4D:
                tStates += 8;
                alu.BIT8((byte) regs.L.getValue(), 1);
                break;

            // bit 1,(hl)
            case 0x4E:
                tStates += 12; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                alu.BIT8_WZ_(val, 1);
            }
                break;

            // bit 1,a
            case 0x4F:
                tStates += 8;
                alu.BIT8((byte) regs.A.getValue(), 1);
                break;

            // bit 2,b
            case 0x50:
                tStates += 8;
                alu.BIT8((byte) regs.B.getValue(), 2);
                break;

            // bit 2,c
            case 0x51:
                tStates += 8;
                alu.BIT8((byte) regs.C.getValue(), 2);
                break;

            // bit 2,d
            case 0x52:
                tStates += 8;
                alu.BIT8((byte) regs.D.getValue(), 2);
                break;

            // bit 2,e
            case 0x53:
                tStates += 8;
                alu.BIT8((byte) regs.E.getValue(), 2);
                break;

            // bit 2,h
            case 0x54:
                tStates += 8;
                alu.BIT8((byte) regs.H.getValue(), 2);
                break;

            // bit 2,l
            case 0x55:
                tStates += 8;
                alu.BIT8((byte) regs.L.getValue(), 2);
                break;

            // bit 2,(hl)
            case 0x56:
                tStates += 12; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                alu.BIT8_WZ_(val, 2);
            }
                break;

            // bit 2,a
            case 0x57:
                tStates += 8;
                alu.BIT8((byte) regs.A.getValue(), 2);
                break;

            // bit 3,b
            case 0x58:
                tStates += 8;
                alu.BIT8((byte) regs.B.getValue(), 3);
                break;

            // bit 3,c
            case 0x59:
                tStates += 8;
                alu.BIT8((byte) regs.C.getValue(), 3);
                break;

            // bit 3,d
            case 0x5A:
                tStates += 8;
                alu.BIT8((byte) regs.D.getValue(), 3);
                break;

            // bit 3,e
            case 0x5B:
                tStates += 8;
                alu.BIT8((byte) regs.E.getValue(), 3);
                break;

            // bit 3,h
            case 0x5C:
                tStates += 8;
                alu.BIT8((byte) regs.H.getValue(), 3);
                break;

            // bit 3,l
            case 0x5D:
                tStates += 8;
                alu.BIT8((byte) regs.L.getValue(), 3);
                break;

            // bit 3,(hl)
            case 0x5E:
                tStates += 12; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                alu.BIT8_WZ_(val, 3);
            }
                break;

            // bit 3,a
            case 0x5F:
                tStates += 8;
                alu.BIT8((byte) regs.A.getValue(), 3);
                break;

            // bit 4,b
            case 0x60:
                tStates += 8;
                alu.BIT8((byte) regs.B.getValue(), 4);
                break;

            // bit 4,c
            case 0x61:
                tStates += 8;
                alu.BIT8((byte) regs.C.getValue(), 4);
                break;

            // bit 4,d
            case 0x62:
                tStates += 8;
                alu.BIT8((byte) regs.D.getValue(), 4);
                break;

            // bit 4,e
            case 0x63:
                tStates += 8;
                alu.BIT8((byte) regs.E.getValue(), 4);
                break;

            // bit 4,h
            case 0x64:
                tStates += 8;
                alu.BIT8((byte) regs.H.getValue(), 4);
                break;

            // bit 4,l
            case 0x65:
                tStates += 8;
                alu.BIT8((byte) regs.L.getValue(), 4);
                break;

            // bit 4,(hl)
            case 0x66:
                tStates += 12; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                alu.BIT8_WZ_(val, 4);
            }
                break;

            // bit 4,a
            case 0x67:
                tStates += 8;
                alu.BIT8((byte) regs.A.getValue(), 4);
                break;

            // bit 5,b
            case 0x68:
                tStates += 8;
                alu.BIT8((byte) regs.B.getValue(), 5);
                break;

            // bit 5,c
            case 0x69:
                tStates += 8;
                alu.BIT8((byte) regs.C.getValue(), 5);
                break;

            // bit 5,d
            case 0x6A:
                tStates += 8;
                alu.BIT8((byte) regs.D.getValue(), 5);
                break;

            // bit 5,e
            case 0x6B:
                tStates += 8;
                alu.BIT8((byte) regs.E.getValue(), 5);
                break;

            // bit 5,h
            case 0x6C:
                tStates += 8;
                alu.BIT8((byte) regs.H.getValue(), 5);
                break;

            // bit 5,l
            case 0x6D:
                tStates += 8;
                alu.BIT8((byte) regs.L.getValue(), 5);
                break;

            // bit 5,(hl)
            case 0x6E:
                tStates += 12; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                alu.BIT8_WZ_(val, 5);
            }
                break;

            // bit 5,a
            case 0x6F:
                tStates += 8;
                alu.BIT8((byte) regs.A.getValue(), 5);
                break;

            // bit 6,b
            case 0x70:
                tStates += 8;
                alu.BIT8((byte) regs.B.getValue(), 6);
                break;

            // bit 6,c
            case 0x71:
                tStates += 8;
                alu.BIT8((byte) regs.C.getValue(), 6);
                break;

            // bit 6,d
            case 0x72:
                tStates += 8;
                alu.BIT8((byte) regs.D.getValue(), 6);
                break;

            // bit 6,e
            case 0x73:
                tStates += 8;
                alu.BIT8((byte) regs.E.getValue(), 6);
                break;

            // bit 6,h
            case 0x74:
                tStates += 8;
                alu.BIT8((byte) regs.H.getValue(), 6);
                break;

            // bit 6,l
            case 0x75:
                tStates += 8;
                alu.BIT8((byte) regs.L.getValue(), 6);
                break;

            // bit 6,(hl)
            case 0x76:
                tStates += 12; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                alu.BIT8_WZ_(val, 6);
            }
                break;

            // bit 6,a
            case 0x77:
                tStates += 8;
                alu.BIT8((byte) regs.A.getValue(), 6);
                break;

            // bit 7,b
            case 0x78:
                tStates += 8;
                alu.BIT8((byte) regs.B.getValue(), 7);
                break;

            // bit 7,c
            case 0x79:
                tStates += 8;
                alu.BIT8((byte) regs.C.getValue(), 7);
                break;

            // bit 7,d
            case 0x7A:
                tStates += 8;
                alu.BIT8((byte) regs.D.getValue(), 7);
                break;

            // bit 7,e
            case 0x7B:
                tStates += 8;
                alu.BIT8((byte) regs.E.getValue(), 7);
                break;

            // bit 7,h
            case 0x7C:
                tStates += 8;
                alu.BIT8((byte) regs.H.getValue(), 7);
                break;

            // bit 7,l
            case 0x7D:
                tStates += 8;
                alu.BIT8((byte) regs.L.getValue(), 7);
                break;

            // bit 7,(hl)
            case 0x7E:
                tStates += 12; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                alu.BIT8_WZ_(val, 7);

            }
                break;

            // bit 7,a
            case 0x7F:
                tStates += 8;
                alu.BIT8((byte) regs.A.getValue(), 7);
                break;

            // res 0,b
            case 0x80:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() & 0xfe));
                break;

            // res 0,c
            case 0x81:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() & 0xfe));
                break;

            // res 0,d
            case 0x82:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() & 0xfe));
                break;

            // res 0,e
            case 0x83:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() & 0xfe));
                break;

            // res 0,h
            case 0x84:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() & 0xfe));
                break;

            // res 0,l
            case 0x85:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() & 0xfe));
                break;

            // res 0,(hl)
            case 0x86:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) & 0xFE));
                break;

            // res 0,a
            case 0x87:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() & 0xfe));
                break;

            // res 1,b
            case 0x88:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() & 0xfd));
                break;

            // res 1,c
            case 0x89:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() & 0xfd));
                break;

            // res 1,d
            case 0x8A:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() & 0xfd));
                break;

            // res 1,e
            case 0x8B:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() & 0xfd));
                break;

            // res 1,h
            case 0x8C:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() & 0xfd));
                break;

            // res 1,l
            case 0x8D:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() & 0xfd));
                break;

            // res 1,(hl)
            case 0x8E:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) & 0xFD));
                break;

            // res 1,a
            case 0x8F:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() & 0xfd));
                break;

            // res 2,b
            case 0x90:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() & 0xfb));
                break;

            // res 2,c
            case 0x91:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() & 0xfb));
                break;

            // res 2,d
            case 0x92:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() & 0xfb));
                break;

            // res 2,e
            case 0x93:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() & 0xfb));
                break;

            // res 2,h
            case 0x94:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() & 0xfb));
                break;

            // res 2,l
            case 0x95:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() & 0xfb));
                break;

            // res 2,(hl)
            case 0x96:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) & 0xFB));
                break;

            // res 2,a
            case 0x97:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() & 0xfb));
                break;

            // res 3,b
            case 0x98:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() & 0xf7));
                break;

            // res 3,c
            case 0x99:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() & 0xf7));
                break;

            // res 3,d
            case 0x9A:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() & 0xf7));
                break;

            // res 3,e
            case 0x9B:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() & 0xf7));
                break;

            // res 3,h
            case 0x9C:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() & 0xf7));
                break;

            // res 3,l
            case 0x9D:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() & 0xf7));
                break;

            // res 3,(hl)
            case 0x9E:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) & 0xF7));
                break;

            // res 3,a
            case 0x9F:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() & 0xf7));
                break;

            // res 4,b
            case 0xA0:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() & 0xef));
                break;

            // res 4,c
            case 0xA1:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() & 0xef));
                break;

            // res 4,d
            case 0xA2:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() & 0xef));
                break;

            // res 4,e
            case 0xA3:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() & 0xef));
                break;

            // res 4,h
            case 0xA4:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() & 0xef));
                break;

            // res 4,l
            case 0xA5:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() & 0xef));
                break;

            // res 4,(hl)
            case 0xA6:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) & 0xEF));
                break;

            // res 4,a
            case 0xA7:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() & 0xef));
                break;

            // res 5,b
            case 0xA8:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() & 0xdf));
                break;

            // res 5,c
            case 0xA9:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() & 0xdf));
                break;

            // res 5,d
            case 0xAA:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() & 0xdf));
                break;

            // res 5,e
            case 0xAB:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() & 0xdf));
                break;

            // res 5,h
            case 0xAC:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() & 0xdf));
                break;

            // res 5,l
            case 0xAD:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() & 0xdf));
                break;

            // res 5,(hl)
            case 0xAE:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) & 0xDF));
                break;

            // res 5,a
            case 0xAF:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() & 0xdf));
                break;

            // res 6,b
            case 0xB0:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() & 0xbf));
                break;

            // res 6,c
            case 0xB1:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() & 0xbf));
                break;

            // res 6,d
            case 0xB2:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() & 0xbf));
                break;

            // res 6,e
            case 0xB3:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() & 0xbf));
                break;

            // res 6,h
            case 0xB4:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() & 0xbf));
                break;

            // res 6,l
            case 0xB5:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() & 0xbf));
                break;

            // res 6,(hl)
            case 0xB6:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) & 0xBF));
                break;

            // res 6,a
            case 0xB7:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() & 0xbf));
                break;

            // res 7,b
            case 0xB8:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() & 0x7f));
                break;

            // res 7,c
            case 0xB9:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() & 0x7f));
                break;

            // res 7,d
            case 0xBA:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() & 0x7f));
                break;

            // res 7,e
            case 0xBB:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() & 0x7f));
                break;

            // res 7,h
            case 0xBC:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() & 0x7f));
                break;

            // res 7,l
            case 0xBD:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() & 0x7f));
                break;

            // res 7,(hl)
            case 0xBE:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) & 0x7F));
                break;

            // res 7,a
            case 0xBF:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() & 0x7f));
                break;

            // set 0,b
            case 0xC0:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() | 0x1));
                break;

            // set 0,c
            case 0xC1:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() | 0x1));
                break;

            // set 0,d
            case 0xC2:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() | 0x1));
                break;

            // set 0,e
            case 0xC3:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() | 0x1));
                break;

            // set 0,h
            case 0xC4:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() | 0x1));
                break;

            // set 0,l
            case 0xC5:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() | 0x1));
                break;

            // set 0,(hl)
            case 0xC6:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) | 0x01));
                break;

            // set 0,a
            case 0xC7:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() | 0x1));
                break;

            // set 1,b
            case 0xC8:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() | 0x2));
                break;

            // set 1,c
            case 0xC9:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() | 0x2));
                break;

            // set 1,d
            case 0xCA:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() | 0x2));
                break;

            // set 1,e
            case 0xCB:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() | 0x2));
                break;

            // set 1,h
            case 0xCC:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() | 0x2));
                break;

            // set 1,l
            case 0xCD:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() | 0x2));
                break;

            // set 1,(hl)
            case 0xCE:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) | 0x02));
                break;

            // set 1,a
            case 0xCF:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() | 0x2));
                break;

            // set 2,b
            case 0xD0:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() | 0x4));
                break;

            // set 2,c
            case 0xD1:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() | 0x4));
                break;

            // set 2,d
            case 0xD2:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() | 0x4));
                break;

            // set 2,e
            case 0xD3:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() | 0x4));
                break;

            // set 2,h
            case 0xD4:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() | 0x4));
                break;

            // set 2,l
            case 0xD5:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() | 0x4));
                break;

            // set 2,(hl)
            case 0xD6:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) | 0x04));
                break;

            // set 2,a
            case 0xD7:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() | 0x4));
                break;

            // set 3,b
            case 0xD8:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() | 0x8));
                break;

            // set 3,c
            case 0xD9:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() | 0x8));
                break;

            // set 3,d
            case 0xDA:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() | 0x8));
                break;

            // set 3,e
            case 0xDB:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() | 0x8));
                break;

            // set 3,h
            case 0xDC:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() | 0x8));
                break;

            // set 3,l
            case 0xDD:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() | 0x8));
                break;

            // set 3,(hl)
            case 0xDE:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) | 0x08));
                break;

            // set 3,a
            case 0xDF:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() | 0x8));
                break;

            // set 4,b
            case 0xE0:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() | 0x10));
                break;

            // set 4,c
            case 0xE1:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() | 0x10));
                break;

            // set 4,d
            case 0xE2:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() | 0x10));
                break;

            // set 4,e
            case 0xE3:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() | 0x10));
                break;

            // set 4,h
            case 0xE4:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() | 0x10));
                break;

            // set 4,l
            case 0xE5:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() | 0x10));
                break;

            // set 4,(hl)
            case 0xE6:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) | 0x10));
                break;

            // set 4,a
            case 0xE7:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() | 0x10));
                break;

            // set 5,b
            case 0xE8:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() | 0x20));
                break;

            // set 5,c
            case 0xE9:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() | 0x20));
                break;

            // set 5,d
            case 0xEA:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() | 0x20));
                break;

            // set 5,e
            case 0xEB:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() | 0x20));
                break;

            // set 5,h
            case 0xEC:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() | 0x20));
                break;

            // set 5,l
            case 0xED:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() | 0x20));
                break;

            // set 5,(hl)
            case 0xEE:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) | 0x20));
                break;

            // set 5,a
            case 0xEF:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() | 0x20));
                break;

            // set 6,b
            case 0xF0:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() | 0x40));
                break;

            // set 6,c
            case 0xF1:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() | 0x40));
                break;

            // set 6,d
            case 0xF2:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() | 0x40));
                break;

            // set 6,e
            case 0xF3:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() | 0x40));
                break;

            // set 6,h
            case 0xF4:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() | 0x40));
                break;

            // set 6,l
            case 0xF5:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() | 0x40));
                break;

            // set 6,(hl)
            case 0xF6:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) | 0x40));
                break;

            // set 6,a
            case 0xF7:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() | 0x40));
                break;

            // set 7,b
            case 0xF8:
                tStates += 8;
                regs.B.setValue((byte) (regs.B.getValue() | 0x80));
                break;

            // set 7,c
            case 0xF9:
                tStates += 8;
                regs.C.setValue((byte) (regs.C.getValue() | 0x80));
                break;

            // set 7,d
            case 0xFA:
                tStates += 8;
                regs.D.setValue((byte) (regs.D.getValue() | 0x80));
                break;

            // set 7,e
            case 0xFB:
                tStates += 8;
                regs.E.setValue((byte) (regs.E.getValue() | 0x80));
                break;

            // set 7,h
            case 0xFC:
                tStates += 8;
                regs.H.setValue((byte) (regs.H.getValue() | 0x80));
                break;

            // set 7,l
            case 0xFD:
                tStates += 8;
                regs.L.setValue((byte) (regs.L.getValue() | 0x80));
                break;

            // set 7,(hl)
            case 0xFE:
                tStates += 15;
                dataBus.memWrite(regs.HL.getValue16(), (byte) (dataBus.memRead(regs.HL.getValue16()) | 0x80));
                break;

            // set 7,a
            case 0xFF:
                tStates += 8;
                regs.A.setValue((byte) (regs.A.getValue() | 0x80));
                break;

        }

    }

    /////////////////////////////////////////////////////////////////////////
    // execInstED():
    // Ejecuta una instrucción ED

    void execInstED() {

        // Obtener la siguiente instrucción (fetch)
        byte op = dataBus.memReadOpCode(regs.getPC() & 0xFFFF);
        regs.setPC((short) (regs.getPC() + 1)); // Incrementar el contador de programa

        // Increment instruction counter register
        REFRESH_CYCLE();

        switch (op & 0xFF) {

            // in b,(c)
            case 0x40:
                tStates += 12;
                IN8(regs.B, regs.BC.getValue16());
                break;

            // out (c),b
            case 0x41:
                tStates += 12;
                dataBus.ioWrite(regs.BC.getValue16(), (byte) regs.B.getValue());
                regs.setWZ((short) (regs.BC.getValue16() + 1));

                break;

            // sbc hl,bc
            case 0x42:
                tStates += 15;
                alu.SBC_R16(regs.BC.getValue16());
                break;

            // ld (NN),bc
            case 0x43:
                tStates += 20;
                write16(read16(regs.getPC()), regs.BC.getValue16());
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // neg
            case 0x44:
            case 0x4c: // undocumented
            case 0x54: // undocumented
            case 0x5c: // undocumented
            case 0x64: // undocumented
            case 0x6c: // undocumented
            case 0x74: // undocumented
            case 0x7c: // undocumented
                tStates += 8; {
                byte val = (byte) regs.A.getValue();
                regs.A.setValue((byte) 0);
                alu.SUB_R8(val);
            }
                break;

            // retn
            case 0x45:
                tStates += 14;
                regs.setiff1A(regs.getiff1B());
                regs.setPC((short) pop16());
                regs.setWZ(regs.getPC());
                // z80_retn(); //Esto es un enlace para los dispositivos que hayan lanzado un
                // NMI, no aplica salvo que 'conectemos' un dispositivo (¿ULA?)
                break;

            case 0x55: // undocumented
            case 0x5d: // undocumented
            case 0x65: // undocumented
            case 0x6d: // undocumented
            case 0x75: // undocumented
            case 0x7d: // undocumented
                tStates += 14;
                regs.setPC((short) pop16());
                regs.setWZ(regs.getPC());
                regs.setiff1A(regs.getiff1B());
                break;

            // im 0
            case 0x46:
            case 0x4e: // undocumented
            case 0x66: // undocumented
            case 0x6e: // undocumented
                tStates += 8;
                regs.setIM((byte) 0);
                break;

            // ld i,a
            case 0x47:
                tStates += 9;
                regs.I.setValue((byte) regs.A.getValue());
                break;

            // in c,(c)
            case 0x48:
                tStates += 12;
                IN8(regs.C, regs.BC.getValue16());
                break;

            // out (c),c
            case 0x49:
                tStates += 12;
                dataBus.ioWrite(regs.BC.getValue16(), (byte) regs.C.getValue());
                regs.setWZ((short) (regs.BC.getValue16() + 1));

                break;

            // adc hl,bc
            case 0x4A:
                tStates += 15;
                alu.ADC_R16(regs.BC.getValue16());
                break;

            // ld bc,(NN)
            case 0x4B:
                tStates += 20;
                regs.BC.setValue(read16(read16(regs.getPC())));
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // reti
            case 0x4D:
                tStates += 14;
                regs.setPC((short) pop16());
                regs.setWZ(regs.getPC());
                break;

            // ld r,a
            case 0x4F:
                tStates += 9;
                regs.R.setValue((byte) regs.A.getValue());
                break;

            // in d,(c)
            case 0x50:
                tStates += 12;
                IN8(regs.D, regs.BC.getValue16());
                break;

            // out (c),d
            case 0x51:
                tStates += 12;
                dataBus.ioWrite(regs.BC.getValue16(), (byte) regs.D.getValue());
                regs.setWZ((short) (regs.BC.getValue16() + 1));

                break;

            // sbc hl,de
            case 0x52:
                tStates += 15;
                alu.SBC_R16(regs.DE.getValue16());
                break;

            // ld (NN),de
            case 0x53:
                tStates += 20;
                write16(read16(regs.getPC()), regs.DE.getValue16());
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // im 1
            case 0x56:
            case 0x76: // undocumented
                tStates += 8;
                regs.setIM((byte) 1);
                break;

            // ld a,i
            case 0x57:
                tStates += 9;
                regs.A.setValue((byte) regs.I.getValue());
                regs.setSF((regs.A.getValue() & 0x80) != 0);
                regs.setZF(regs.A.getValue() == 0);
                regs.setHF(false);
                regs.setNF(false);
                regs.setPF(regs.getiff1B());
                regs.setF3((regs.A.getValue() & 0x08) != 0);
                regs.setF5((regs.A.getValue() & 0x20) != 0);
                break;

            // in e,(c)
            case 0x58:
                tStates += 12;
                IN8(regs.E, regs.BC.getValue16());
                break;

            // out (c),e
            case 0x59:
                tStates += 12;
                dataBus.ioWrite(regs.BC.getValue16(), (byte) regs.E.getValue());
                regs.setWZ((short) (regs.BC.getValue16() + 1));

                break;

            // adc hl,de
            case 0x5A:
                tStates += 15;
                alu.ADC_R16(regs.DE.getValue16());
                break;

            // ld de,(NN)
            case 0x5B:
                tStates += 20;
                regs.DE.setValue(read16(read16(regs.getPC())));
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // im 2
            case 0x5E:
            case 0x7e: // undocumented
                tStates += 8;
                regs.setIM((byte) 2);
                break;

            // ld a,r
            case 0x5F:
                tStates += 9;
                regs.A.setValue((byte) regs.R.getValue());
                regs.setSF((regs.A.getValue() & 0x80) != 0);
                regs.setZF(regs.A.getValue() == 0);
                regs.setHF(false);
                regs.setNF(false);
                regs.setPF(regs.getiff1B());
                // quizas no aplica
                regs.setF3((regs.A.getValue() & 0x08) != 0);
                regs.setF5((regs.A.getValue() & 0x20) != 0);
                break;

            // in h,(c)
            case 0x60:
                tStates += 12;
                IN8(regs.H, regs.BC.getValue16());
                break;

            // out (c),h
            case 0x61:
                tStates += 12;
                dataBus.ioWrite(regs.BC.getValue16(), (byte) regs.H.getValue());
                regs.setWZ((short) (regs.BC.getValue16() + 1));

                break;

            // sbc hl,hl
            case 0x62:
                tStates += 15;
                alu.SBC_R16(regs.HL.getValue16());
                break;

            // ld (nn),hl
            case 0x63:
                tStates += 20;
                write16(read16(regs.getPC()), regs.HL.getValue16());
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // rrd
            case 0x67:
                tStates += 18; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                dataBus.memWrite(regs.HL.getValue16(),
                        (byte) ((byte) ((val >> 4) & 0x0f) | (byte) ((regs.A.getValue() << 4) & 0xF0)));

                regs.A.setValue((byte) (regs.A.getValue() & 0xF0));
                regs.A.setValue((byte) (regs.A.getValue() | (val & 0x0F)));
                regs.setSF((regs.A.getValue() & 0x80) != 0);
                regs.setZF(regs.A.getValue() == 0);
                regs.setHF(false);
                regs.setNF(false);
                regs.setPF(parityTable[regs.A.getValue() & 0xFF]);
                regs.setF3((regs.A.getValue() & 0x008) != 0);
                regs.setF5((regs.A.getValue() & 0x020) != 0);
                regs.setWZ((short) (regs.HL.getValue16() + 1));
            }
                break;

            // in l,(c)
            case 0x68:
                tStates += 12;
                IN8(regs.L, regs.BC.getValue16());
                break;

            // out (c),l
            case 0x69:
                tStates += 12;
                dataBus.ioWrite(regs.BC.getValue16(), (byte) regs.L.getValue());
                regs.setWZ((short) (regs.BC.getValue16() + 1));

                break;

            // adc hl,hl
            case 0x6A:
                tStates += 15;
                alu.ADC_R16(regs.HL.getValue16());
                break;

            // ld hl,(NN)
            case 0x6B:
                tStates += 20;
                regs.HL.setValue(read16(read16(regs.getPC())));
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // rld
            case 0x6F:
                tStates += 18; {
                byte aux = dataBus.memRead(regs.HL.getValue16());
                dataBus.memWrite(regs.HL.getValue16(),
                        (byte) ((byte) ((aux << 4) & 0xF0) | (byte) ((byte) regs.A.getValue() & 0x0F)));
                regs.A.setValue((byte) (regs.A.getValue() & 0xF0));
                regs.A.setValue((byte) (regs.A.getValue() | ((aux >> 4) & 0x0f)));
                regs.setSF((regs.A.getValue() & 0x80) != 0);
                regs.setZF(regs.A.getValue() == 0);
                regs.setHF(false);
                regs.setNF(false);
                regs.setPF(parityTable[regs.A.getValue() & 0xFF]);
                regs.setF3((regs.A.getValue() & 0x008) != 0);
                regs.setF5((regs.A.getValue() & 0x020) != 0);
                regs.setWZ((short) (regs.HL.getValue16() + 1));
            }
                break;

            // in f,(c)
            // in (c)
            case 0x70:
                tStates += 12; {
                regs.setWZ((short) (regs.BC.getValue16() + 1));
                byte val = dataBus.ioRead(regs.BC.getValue16());
                regs.setSF((val & 0x80) != 0);
                regs.setZF(val == 0);
                regs.setHF(false);
                regs.setPF(parityTable[val & 0xFF]);
                regs.setNF(false);
                regs.setF3((val & 0x08) != 0);
                regs.setF5((val & 0x20) != 0);
            }
                break;

            // out (c),0
            case 0x71:
                tStates += 12;
                dataBus.ioWrite(regs.BC.getValue16(), (byte) 0);
                regs.setWZ((short) (regs.BC.getValue16() + 1));
                break;

            // sbc hl,sp
            case 0x72:
                tStates += 15;
                alu.SBC_R16(regs.getSP());
                break;

            // ld (NN),sp
            case 0x73:
                tStates += 20;
                write16(read16(regs.getPC()), regs.getSP());
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // undocumented nop's
            case 0x77:
            case 0x7f:
                tStates += 8;
                break;

            // in a,(c)
            case 0x78:
                tStates += 12;
                IN8(regs.A, regs.BC.getValue16());
                break;

            // out (c),a
            case 0x79:
                tStates += 12;
                dataBus.ioWrite(regs.BC.getValue16(), (byte) regs.A.getValue());
                regs.setWZ((short) (regs.BC.getValue16() + 1));
                break;

            // adc hl,sp
            case 0x7A:
                tStates += 15;
                alu.ADC_R16(regs.getSP());
                break;

            // ld sp,(NN)
            case 0x7B:
                tStates += 20;
                regs.setSP(read16(read16(regs.getPC())));
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // ldi
            case 0xA0:
                tStates += 16;
                LD_BLOCK(true); // increment
                break;

            // cpi
            case 0xA1:
                tStates += 16; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                alu.CMP_R8_NOFLAGS((byte) regs.A.getValue(), val);
                regs.HL.setValue((short) (regs.HL.getValue16() + 1));
                regs.BC.setValue((short) (regs.BC.getValue16() - 1));

                regs.setPF(regs.BC.getValue16() != 0);
                byte aux = (byte) (regs.A.getValue() - val - (regs.getHF() ? 1 : 0));
                regs.setF3((aux & 0x08) != 0);
                regs.setF5((aux & 0x02) != 0);
                regs.setWZ((short) (regs.getWZ() + 1));

            }
                break;

            // ini
            case 0xA2:
                tStates += 16;
                IN_BLOCK(true);
                break;

            // outi
            case 0xA3:
                tStates += 16;
                OUT_BLOCK(true);
                break;

            // ldd
            case 0xA8:
                tStates += 16;
                LD_BLOCK(false); // decrement
                break;

            // cpd
            case 0xA9:
                tStates += 16; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                alu.CMP_R8_NOFLAGS((byte) regs.A.getValue(), val);
                regs.HL.setValue((short) (regs.HL.getValue16() - 1));
                regs.BC.setValue((short) (regs.BC.getValue16() - 1));
                regs.setPF(regs.BC.getValue16() != 0);
                byte aux = (byte) (regs.A.getValue() - val - (regs.getHF() ? 1 : 0));
                regs.setF3((aux & 0x08) != 0);
                regs.setF5((aux & 0x02) != 0);
                regs.setWZ((short) (regs.getWZ() - 1));
                }
                break;

            // ind
            case 0xAA:
                tStates += 16;
                IN_BLOCK(false);
                break;

            // outd

            case 0xAB:
                tStates += 16;
                OUT_BLOCK(false);
                break;

            // ldir
            case 0xB0:
                tStates += 16;
                LD_BLOCK(true); // increment
                if (regs.BC.getValue16() != 0) {
                    tStates += 5;
                    regs.setWZ((short) (regs.getPC() - 1));
                    regs.setPC((short) (regs.getPC() - 2)); // Repeat instruction.
                    regs.setF3((regs.getPC() & 0x0800) != 0);
                    regs.setF5((regs.getPC() & 0x2000) != 0);
                }
                break;

            // cpir
            case 0xB1:
                tStates += 16; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                alu.CMP_R8_NOFLAGS((byte) regs.A.getValue(), val);
                regs.HL.setValue((short) (regs.HL.getValue16() + 1));
                regs.BC.setValue((short) (regs.BC.getValue16() - 1));
                regs.setPF(regs.BC.getValue16() != 0);
                // Take the value of register A, subtract the value of the memory address, and
                // finally subtract the value of HF flag,
                // which is set or reset by the hypothetical CP (HL). So, n = A - (HL) - HF.
                byte aux = (byte) (regs.A.getValue() - val - (regs.getHF() ? 1 : 0));
                regs.setF3((aux & 0x08) != 0);
                regs.setF5((aux & 0x02) != 0);
                regs.setWZ((short) (regs.getWZ() + 1));
                if (regs.getPF() && !regs.getZF()) {
                    tStates += 5;
                    regs.setPC((short) (regs.getPC() - 2)); // repeat instruction.
                    regs.setWZ((short) (regs.getPC() + 1));
                    regs.setF3((regs.getPC() & 0x0800) != 0);
                    regs.setF5((regs.getPC() & 0x2000) != 0);
                }
            }
                break;

            // inir
            case 0xB2:
                tStates += 16;
                IN_BLOCK(true);
                if (!regs.getZF()) {
                    tStates += 5;
                    regs.setPC((short) (regs.getPC() - 2)); // repeat instruction.
                    regs.setWZ((short) (regs.getPC() + 1)); // PC +1 y de acuerdo al emulador de Jsanchezv
                    INxROUTxRFlags_BLOCK();
                }
                break;

            // otir
            case 0xB3:
                tStates += 16;
                OUT_BLOCK(true);
                if (!regs.getZF()) {
                    tStates += 5;
                    regs.setPC((short) (regs.getPC() - 2)); // repeat instruction.
                    INxROUTxRFlags_BLOCK();
                }
                break;

            // lddr
            case 0xB8:
                tStates += 16;
                LD_BLOCK(false); // decrement

                if (regs.BC.getValue16() != 0) {
                    tStates += 5;
                    regs.setWZ((short) (regs.getPC() - 1)); // PC +1 por FUSE PC -1
                    regs.setPC((short) (regs.getPC() - 2)); // repeat instruction.
                    regs.setF3((regs.getPC() & 0x0800) != 0);
                    regs.setF5((regs.getPC() & 0x2000) != 0);
                }
                break;

            // cpdr
            case 0xB9:
                tStates += 16; {
                byte val = dataBus.memRead(regs.HL.getValue16());
                regs.HL.setValue((short) (regs.HL.getValue16() - 1));
                alu.CMP_R8_NOFLAGS((byte) regs.A.getValue(), val);
                regs.BC.setValue((short) (regs.BC.getValue16() - 1));
                regs.setPF(regs.BC.getValue16() != 0);
                byte aux = (byte) (regs.A.getValue() - val - (regs.getHF() ? 1 : 0));
                regs.setF3((aux & 0x08) != 0);
                regs.setF5((aux & 0x02) != 0);
                regs.setWZ((short) (regs.getWZ() - 1));
                if (regs.getPF() && !regs.getZF()) {
                    tStates += 5;
                    regs.setPC((short) (regs.getPC() - 2)); // repeat instruction.
                    regs.setWZ((short) (regs.getPC() + 1));
                    regs.setF3((regs.getPC() & 0x0800) != 0);
                    regs.setF5((regs.getPC() & 0x2000) != 0);
                }
            }
                break;

            // indr
            case 0xBA:
                tStates += 16;
                IN_BLOCK(false);
                if (!regs.getZF()) {
                    tStates += 5;
                    regs.setPC((short) (regs.getPC() - 2)); // repeat instruction.
                    regs.setWZ((short) (regs.getPC() + 1)); // PC +1 y de acuerdo al emulador de Jsanchezv
                    INxROUTxRFlags_BLOCK();
                }
                break;

            // otdr
            case 0xBB:
                tStates += 16;
                OUT_BLOCK(false);
                if (!regs.getZF()) {
                    tStates += 5;
                    regs.setPC((short) (regs.getPC() - 2)); // repeat instruction.
                    INxROUTxRFlags_BLOCK();
                }
                break;

            case 0xFB:
                tStates += 8;
                break;

            case 0xFC:
                tStates += 8;
                break;

            case 0xFD:
                tStates += 8;
                break;

            default:
                tStates += 8;
                break;
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // execInstXX():
    // Ejecuta una instrucción con prefijo DD o FD.
    // Hace uso del registro falso XX (copia de IX o IY según el caso)
    
    void execInstXX() {

        // Obtener la siguiente instrucción (fetch)
        byte op = dataBus.memReadOpCode(regs.getPC() & 0xFFFF);
        regs.setPC((short) (regs.getPC() + 1)); // Incrementar el contador de programa

        // Increment instruction counter register
        REFRESH_CYCLE();

        switch (op & 0xFF) {

            // add xx,bc
            case 0x09:
                tStates += 15;
                alu.ADD_R16(regs.XX, regs.BC.getValue16());
                break;

            // add xx,de
            case 0x19:
                tStates += 15;
                alu.ADD_R16(regs.XX, regs.DE.getValue16());
                break;

            // ld xx,NN
            case 0x21:
                tStates += 14;
                regs.XX.setValue(read16(regs.getPC()));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // ld (NN),xx
            case 0x22:
                tStates += 20;
                write16(read16(regs.getPC()), regs.XX.getValue16());
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // inc xx
            case 0x23:
                tStates += 10;
                regs.XX.setValue((short) (regs.XX.getValue16() + 1));
                break;

            // inc hx
            case 0x24:
                tStates += 8;
                alu.INC_R8(regs.HX);
                break;

            // dec hx
            case 0x25:
                tStates += 8;
                alu.DEC_R8(regs.HX);
                break;

            // ld hx,N
            case 0x26:
                tStates += 11;
                regs.HX.setValue(dataBus.memRead(regs.getPC()));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // add xx,xx
            case 0x29:
                tStates += 15;
                alu.ADD_R16(regs.XX, regs.XX.getValue16());
                break;

            // ld xx,(NN)
            case 0x2A:
                tStates += 20;
                regs.XX.setValue(read16(read16(regs.getPC())));
                regs.setWZ((short) (read16(regs.getPC()) + 1));
                regs.setPC((short) (regs.getPC() + 2));
                break;

            // dec xx
            case 0x2B:
                tStates += 10;
                regs.XX.setValue((short) (regs.XX.getValue16() - 1));
                break;

            // inc lx
            case 0x2C:
                tStates += 8;
                alu.INC_R8(regs.LX);
                break;

            // dec lx
            case 0x2D:
                tStates += 8;
                alu.DEC_R8(regs.LX);
                break;

            // ld lx,N
            case 0x2E:
                tStates += 11;
                regs.LX.setValue(dataBus.memRead(regs.getPC()));
                regs.setPC((short) (regs.getPC() + 1));
                break;

            // inc (xx+d)
            case 0x34:
                tStates += 23; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                Register regval = new Register(dataBus.memRead(addr));
                alu.INC_R8(regval);
                dataBus.memWrite(addr, (byte) regval.getValue());
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // dec (xx+d)
            case 0x35:
                tStates += 23; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                byte val = dataBus.memRead(addr);
                Register regval = new Register(dataBus.memRead(addr));
                alu.DEC_R8(regval);
                dataBus.memWrite(addr, (byte) regval.getValue());
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld (xx+d),N
            case 0x36:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                dataBus.memWrite(addr, dataBus.memRead(regs.getPC() + 1));

                regs.setPC((short) (regs.getPC() + 2));
            }
                break;

            // add xx,sp
            case 0x39:
                tStates += 15;
                alu.ADD_R16(regs.XX, regs.getSP());
                break;

            // ld b,hx
            case 0x44:
                tStates += 8;
                regs.B.setValue((byte) regs.HX.getValue());
                break;

            // ld b,lx
            case 0x45:
                tStates += 8;
                regs.B.setValue((byte) regs.LX.getValue());
                break;

            // ld b,(xx+d)
            case 0x46:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.B.setValue(dataBus.memRead(addr));
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld c,hx
            case 0x4C:
                tStates += 8;
                regs.C.setValue((byte) regs.HX.getValue());
                break;

            // ld c,lx
            case 0x4D:
                tStates += 8;
                regs.C.setValue((byte) regs.LX.getValue());
                break;

            // ld c,(xx+d)
            case 0x4E:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.C.setValue(dataBus.memRead(addr));
                ;
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld d,hx
            case 0x54:
                tStates += 8;
                regs.D.setValue((byte) regs.HX.getValue());
                break;

            // ld d,lx
            case 0x55:
                tStates += 8;
                regs.D.setValue((byte) regs.LX.getValue());
                break;

            // ld d,(xx+d)
            case 0x56:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.D.setValue(dataBus.memRead(addr));
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld e,hx
            case 0x5C:
                tStates += 8;
                regs.E.setValue((byte) regs.HX.getValue());
                break;

            // ld e,lx
            case 0x5D:
                tStates += 8;
                regs.E.setValue((byte) regs.LX.getValue());
                break;

            // ld e,(xx+d)
            case 0x5E:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.E.setValue(dataBus.memRead(addr));
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld hx,b
            case 0x60:
                tStates += 8;
                regs.HX.setValue((byte) regs.B.getValue());
                break;

            // ld hx,c
            case 0x61:
                tStates += 8;
                regs.HX.setValue((byte) regs.C.getValue());
                break;

            // ld hx,d
            case 0x62:
                tStates += 8;
                regs.HX.setValue((byte) regs.D.getValue());
                break;

            // ld hx,e
            case 0x63:
                tStates += 8;
                regs.HX.setValue((byte) regs.E.getValue());
                break;

            // ld hx,hx
            case 0x64:
                tStates += 8;
                break;

            // ld hx,lx
            case 0x65:
                tStates += 8;
                regs.HX.setValue((byte) regs.LX.getValue());
                break;

            // ld h,(xx+d)
            case 0x66:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.H.setValue(dataBus.memRead(addr));
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld hx,a
            case 0x67:
                tStates += 8;
                regs.HX.setValue((byte) regs.A.getValue());
                break;

            // ld lx,b
            case 0x68:
                tStates += 8;
                regs.LX.setValue((byte) regs.B.getValue());
                break;

            // ld lx,c
            case 0x69:
                tStates += 8;
                regs.LX.setValue((byte) regs.C.getValue());
                break;

            // ld lx,d
            case 0x6A:
                tStates += 8;
                regs.LX.setValue((byte) regs.D.getValue());
                break;

            // ld lx,e
            case 0x6B:
                tStates += 8;
                regs.LX.setValue((byte) regs.E.getValue());
                break;

            // ld lx,hx
            case 0x6C:
                tStates += 8;
                regs.LX.setValue((byte) regs.HX.getValue());
                break;

            // ld lx,lx
            case 0x6D:
                tStates += 8;
                break;

            // ld l,(xx+d)
            case 0x6E:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.L.setValue(dataBus.memRead(addr));
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld lx,a
            case 0x6F:
                tStates += 8;
                regs.LX.setValue((byte) regs.A.getValue());
                break;

            // ld (xx+d),b
            case 0x70:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                dataBus.memWrite(addr, (byte) regs.B.getValue());
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld (xx+d),c
            case 0x71:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                dataBus.memWrite(addr, (byte) regs.C.getValue());
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld (xx+d),d
            case 0x72:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                dataBus.memWrite(addr, (byte) regs.D.getValue());
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld (xx+d),e
            case 0x73:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                dataBus.memWrite(addr, (byte) regs.E.getValue());
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld (xx+d),h
            case 0x74:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                dataBus.memWrite(addr, (byte) regs.H.getValue());
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld (xx+d),l
            case 0x75:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                dataBus.memWrite(addr, (byte) regs.L.getValue());
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld (xx+d),a
            case 0x77:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                dataBus.memWrite(addr, (byte) regs.A.getValue());
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // ld a,hx
            case 0x7C:
                tStates += 8;
                regs.A.setValue((byte) regs.HX.getValue());
                break;

            // ld a,lx
            case 0x7D:
                tStates += 8;
                regs.A.setValue((byte) regs.LX.getValue());
                break;

            // ld a,(xx+d)
            case 0x7E:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.A.setValue(dataBus.memRead(addr));
                regs.setPC((short) (regs.getPC() + 1));
            }
                break;

            // add a,hx
            case 0x84:
                tStates += 8;
                alu.ADD_R8((byte) regs.HX.getValue());
                break;

            // add a,lx
            case 0x85:
                tStates += 8;
                alu.ADD_R8((byte) regs.LX.getValue());
                break;

            // add a,(xx+d)
            case 0x86:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.setPC((short) (regs.getPC() + 1));
                byte val = dataBus.memRead(addr);
                alu.ADD_R8(val);
            }
                break;

            // adc a,hx
            case 0x8C:
                tStates += 8;
                alu.ADC_R8((byte) regs.HX.getValue());
                break;

            // adc a,lx
            case 0x8D:
                tStates += 8;
                alu.ADC_R8((byte) regs.LX.getValue());
                break;

            // adc a,(xx+d)
            case 0x8E:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.setPC((short) (regs.getPC() + 1));
                byte val = dataBus.memRead(addr);
                alu.ADC_R8(val);
            }
                break;

            // sub hx
            case 0x94:
                tStates += 8;
                alu.SUB_R8((byte) regs.HX.getValue());
                break;

            // sub lx
            case 0x95:
                tStates += 8;
                alu.SUB_R8((byte) regs.LX.getValue());
                break;

            // sub (xx+d)
            case 0x96:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.setPC((short) (regs.getPC() + 1));
                byte val = dataBus.memRead(addr);
                alu.SUB_R8(val);
            }
                break;

            // sbc a,hx
            case 0x9C:
                tStates += 8;
                alu.SBC_R8((byte) regs.HX.getValue());
                break;

            // sbc a,lx
            case 0x9D:
                tStates += 8;
                alu.SBC_R8((byte) regs.LX.getValue());
                break;

            // sbc a,(xx+d)
            case 0x9E:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.setPC((short) (regs.getPC() + 1));
                byte val = dataBus.memRead(addr);
                alu.SBC_R8((byte) val);
            }
                break;

            // and hx
            case 0xA4:
                tStates += 8;
                alu.AND_R8((byte) regs.HX.getValue());
                break;

            // and lx
            case 0xA5:
                tStates += 8;
                alu.AND_R8((byte) regs.LX.getValue());
                break;

            // and (xx+d)
            case 0xA6:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.setPC((short) (regs.getPC() + 1));
                byte val = dataBus.memRead(addr);
                alu.AND_R8(val);
            }
                break;

            // xor hx
            case 0xAC:
                tStates += 8;
                alu.XOR_R8((byte) regs.HX.getValue());
                break;

            // xor lx
            case 0xAD:
                tStates += 8;
                alu.XOR_R8((byte) regs.LX.getValue());
                break;

            // xor (xx+d)
            case 0xAE:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.setPC((short) (regs.getPC() + 1));
                byte val = dataBus.memRead(addr);
                alu.XOR_R8(val);
            }
                break;

            // or hx
            case 0xB4:
                tStates += 8;
                alu.OR_R8((byte) regs.HX.getValue());
                break;

            // or lx
            case 0xB5:
                tStates += 8;
                alu.OR_R8((byte) regs.LX.getValue());
                break;

            // or (xx+d)
            case 0xB6:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.setPC((short) (regs.getPC() + 1));
                byte val = dataBus.memRead(addr);
                alu.OR_R8(val);
            }
                break;

            // cp hx
            case 0xBC:
                tStates += 8;
                alu.CMP_R8((byte) regs.HX.getValue());
                break;

            // cp lx
            case 0xBD:
                tStates += 8;
                alu.CMP_R8((byte) regs.LX.getValue());
                break;

            // cp (xx+d)
            case 0xBE:
                tStates += 19; {
                short addr = (short) (regs.XX.getValue16() + (byte) dataBus.memRead(regs.getPC()));
                regs.setWZ(addr);
                regs.setPC((short) (regs.getPC() + 1));
                byte val = dataBus.memRead(addr);
                alu.CMP_R8(val);
            }
                break;

            // Index register with 0xCB handling
            case 0xCB:
                execInstXXCB();
                break;

            // pop xx
            case 0xE1:
                tStates += 14;
                regs.XX.setValue(pop16());
                break;

            // ex (sp),xx
            case 0xE3:
                tStates += 23; {
                short val = read16(regs.getSP());
                write16(regs.getSP(), regs.XX.getValue16());
                regs.XX.setValue(val);
                regs.setWZ(val);
            }
                break;

            // push XX
            case 0xE5:
                tStates += 15;
                push16(regs.XX.getValue16());
                break;

            // jp (xx)
            case 0xE9:
                tStates += 8;
                regs.setPC(regs.XX.getValue16());
                break;

            // ld sp,xx
            case 0xF9:
                tStates += 10;
                regs.setSP(regs.XX.getValue16());
                break;

            case 0xDD:
            case 0xFD:
                tStates += 4;
                regs.setPC((short) (regs.getPC() - 1)); // Back to the second DD/FD
                REFRESH_CYCLE_BACK(); // Back to the same refresh cycle
                break;

            case 0xEB:
            case 0xED:
                tStates += 8;
                break;

            // Unimplemented opcodes do the same as the unprefixed versions.
            // Roll back PC register and re-evaluate.
            default:
                tStates += 4;
                regs.setPC((short) (regs.getPC() - 1)); // Back to the non-prefixed opcode
                REFRESH_CYCLE_BACK(); // Back to the same refresh cycle
        }

    }

    /////////////////////////////////////////////////////////////////////////
    // execInstXXCB():
    // Ejecuta una instrucción DDCB/FDCB

    void execInstXXCB() {

        // Fetch offset
        byte offset = dataBus.memRead(regs.getPC());
        regs.setPC((short) (regs.getPC() + 1));
        short xxd = (short) (regs.XX.getValue16() + offset);
        regs.setWZ(xxd); // En la cpu real todas las operaciones XXCB se hacen a partir de WZ

        // Obtener la siguiente instrucción (fetch)
        byte op = dataBus.memRead(regs.getPC() & 0xFFFF);
        regs.setPC((short) (regs.getPC() + 1)); // Incrementar el contador de programa

        switch (op & 0xFF) {

            // rlc (xx+d),b
            case 0x00:
                tStates += 23;
                regs.B.setValue(dataBus.memRead(xxd));
                alu.RLC8(regs.B);
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // rlc (xx+d),c
            case 0x01:
                tStates += 23;
                regs.C.setValue(dataBus.memRead(xxd));
                alu.RLC8(regs.C);
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // rlc (xx+d),d
            case 0x02:
                tStates += 23;
                regs.D.setValue(dataBus.memRead(xxd));
                alu.RLC8(regs.D);
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // rlc (xx+d),e
            case 0x03:
                tStates += 23;
                regs.E.setValue(dataBus.memRead(xxd));
                alu.RLC8(regs.E);
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // rlc (xx+d),h
            case 0x04:
                tStates += 23;
                regs.H.setValue(dataBus.memRead(xxd));
                alu.RLC8(regs.H);
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // rlc (xx+d),l
            case 0x05:
                tStates += 23;
                regs.L.setValue(dataBus.memRead(xxd));
                alu.RLC8(regs.L);
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // rlc (xx+d)
            case 0x06:
                tStates += 23; {
                Register regval = new Register(dataBus.memRead(xxd));
                alu.RLC8(regval);
                dataBus.memWrite(xxd, (byte) regval.getValue());
            }
                break;

            // rlc (xx+d),a
            case 0x07:
                tStates += 23;
                regs.A.setValue(dataBus.memRead(xxd));
                alu.RLC8(regs.A);
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // rrc (xx+d),b
            case 0x08:
                tStates += 23;
                regs.B.setValue(dataBus.memRead(xxd));
                alu.RRC8(regs.B);
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // rrc (xx+d),c
            case 0x09:
                tStates += 23;
                regs.C.setValue(dataBus.memRead(xxd));
                alu.RRC8(regs.C);
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // rrc (xx+d),d
            case 0x0A:
                tStates += 23;
                regs.D.setValue(dataBus.memRead(xxd));
                alu.RRC8(regs.D);
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // rrc (xx+d),e
            case 0x0B:
                tStates += 23;
                regs.E.setValue(dataBus.memRead(xxd));
                alu.RRC8(regs.E);
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // rrc (xx+d),h
            case 0x0C:
                tStates += 23;
                regs.H.setValue(dataBus.memRead(xxd));
                alu.RRC8(regs.H);
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // rrc (xx+d),l
            case 0x0D:
                tStates += 23;
                regs.L.setValue(dataBus.memRead(xxd));
                alu.RRC8(regs.L);
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // rrc (xx+d)
            case 0x0E:
                tStates += 23; {
                Register regval = new Register(dataBus.memRead(xxd));
                alu.RRC8(regval);
                dataBus.memWrite(xxd, (byte) regval.getValue());
            }
                break;

            // rrc a
            case 0x0F:
                tStates += 23;
                regs.A.setValue(dataBus.memRead(xxd));
                alu.RRC8(regs.A);
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // rl (xx+d),b
            case 0x10:
                tStates += 23;
                regs.B.setValue(dataBus.memRead(xxd));
                alu.RL8(regs.B);
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // rl (xx+d),c
            case 0x11:
                tStates += 23;
                regs.C.setValue(dataBus.memRead(xxd));
                alu.RL8(regs.C);
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // rl (xx+d),d
            case 0x12:
                tStates += 23;
                regs.D.setValue(dataBus.memRead(xxd));
                alu.RL8(regs.D);
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // rl (xx+d),e
            case 0x13:
                tStates += 23;
                regs.E.setValue(dataBus.memRead(xxd));
                alu.RL8(regs.E);
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // rl (xx+d),h
            case 0x14:
                tStates += 23;
                regs.H.setValue(dataBus.memRead(xxd));
                alu.RL8(regs.H);
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // rl (xx+d),l
            case 0x15:
                tStates += 23;
                regs.L.setValue(dataBus.memRead(xxd));
                alu.RL8(regs.L);
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // rl (xx+d)
            case 0x16:
                tStates += 23; {
                Register regval = new Register(dataBus.memRead(xxd));
                alu.RL8(regval);
                dataBus.memWrite(xxd, (byte) regval.getValue());
            }
                break;

            // rl (xx+d),a
            case 0x17:
                tStates += 23;
                regs.A.setValue(dataBus.memRead(xxd));
                alu.RL8(regs.A);
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // rr (xx+d),b
            case 0x18:
                tStates += 23;
                regs.B.setValue(dataBus.memRead(xxd));
                alu.RR8(regs.B);
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // rr (xx+d),c
            case 0x19:
                tStates += 23;
                regs.C.setValue(dataBus.memRead(xxd));
                alu.RR8(regs.C);
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // rr (xx+d),d
            case 0x1A:
                tStates += 23;
                regs.D.setValue(dataBus.memRead(xxd));
                alu.RR8(regs.D);
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // rr (xx+d),e
            case 0x1B:
                tStates += 23;
                regs.E.setValue(dataBus.memRead(xxd));
                alu.RR8(regs.E);
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // rr (xx+d),h
            case 0x1C:
                tStates += 23;
                regs.H.setValue(dataBus.memRead(xxd));
                alu.RR8(regs.H);
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // rr (xx+d),l
            case 0x1D:
                tStates += 23;
                regs.L.setValue(dataBus.memRead(xxd));
                alu.RR8(regs.L);
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // rr (xx+d)
            case 0x1E:
                tStates += 23; {
                Register regval = new Register(dataBus.memRead(xxd));
                alu.RR8(regval);
                dataBus.memWrite(xxd, (byte) regval.getValue());
            }
                break;

            // rr (xx+d),a
            case 0x1F:
                tStates += 23;
                regs.A.setValue(dataBus.memRead(xxd));
                alu.RR8(regs.A);
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // sla (xx+d),b
            case 0x20:
                tStates += 23;
                regs.B.setValue(dataBus.memRead(xxd));
                alu.SLA8(regs.B);
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // sla (xx+d),c
            case 0x21:
                tStates += 23;
                regs.C.setValue(dataBus.memRead(xxd));
                alu.SLA8(regs.C);
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // sla (xx+d),d
            case 0x22:
                tStates += 23;
                regs.D.setValue(dataBus.memRead(xxd));
                alu.SLA8(regs.D);
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // sla (xx+d),e
            case 0x23:
                tStates += 23;
                regs.E.setValue(dataBus.memRead(xxd));
                alu.SLA8(regs.E);
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // sla (xx+d),h
            case 0x24:
                tStates += 23;
                regs.H.setValue(dataBus.memRead(xxd));
                alu.SLA8(regs.H);
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // sla (xx+d),l
            case 0x25:
                tStates += 23;
                regs.L.setValue(dataBus.memRead(xxd));
                alu.SLA8(regs.L);
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // sla (xx+d)
            case 0x26:
                tStates += 23; {
                Register regval = new Register(dataBus.memRead(xxd));
                alu.SLA8(regval);
                dataBus.memWrite(xxd, (byte) regval.getValue());
            }
                break;

            // sla (xx+d),a
            case 0x27:
                tStates += 23;
                regs.A.setValue(dataBus.memRead(xxd));
                alu.SLA8(regs.A);
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // sra (xx+d),b
            case 0x28:
                tStates += 23;
                regs.B.setValue(dataBus.memRead(xxd));
                alu.SRA8(regs.B);
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // sra (xx+d),c
            case 0x29:
                tStates += 23;
                regs.C.setValue(dataBus.memRead(xxd));
                alu.SRA8(regs.C);
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // sra (xx+d),d
            case 0x2A:
                tStates += 23;
                regs.D.setValue(dataBus.memRead(xxd));
                alu.SRA8(regs.D);
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // sra (xx+d),e
            case 0x2B:
                tStates += 23;
                regs.E.setValue(dataBus.memRead(xxd));
                alu.SRA8(regs.E);
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // sra (xx+d),h
            case 0x2C:
                tStates += 23;
                regs.H.setValue(dataBus.memRead(xxd));
                alu.SRA8(regs.H);
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // sra (xx+d),l
            case 0x2D:
                tStates += 23;
                regs.L.setValue(dataBus.memRead(xxd));
                alu.SRA8(regs.L);
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // sra (xx+d)
            case 0x2E:
                tStates += 23; {
                Register regval = new Register(dataBus.memRead(xxd));
                alu.SRA8(regval);
                dataBus.memWrite(xxd, (byte) regval.getValue());
            }
                break;

            // sra (xx+d),a
            case 0x2F:
                tStates += 23;
                regs.A.setValue(dataBus.memRead(xxd));
                alu.SRA8(regs.A);
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // sli (xx+d),b
            case 0x30:
                tStates += 23;
                regs.B.setValue(dataBus.memRead(xxd));
                alu.SLI8(regs.B);
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // sli (xx+d),c
            case 0x31:
                tStates += 23;
                regs.C.setValue(dataBus.memRead(xxd));
                alu.SLI8(regs.C);
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // sli (xx+d),d
            case 0x32:
                tStates += 23;
                regs.D.setValue(dataBus.memRead(xxd));
                alu.SLI8(regs.D);
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // sli (xx+d),e
            case 0x33:
                tStates += 23;
                regs.E.setValue(dataBus.memRead(xxd));
                alu.SLI8(regs.E);
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // sli (xx+d),h
            case 0x34:
                tStates += 23;
                regs.H.setValue(dataBus.memRead(xxd));
                alu.SLI8(regs.H);
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // sli (xx+d),l
            case 0x35:
                tStates += 23;
                regs.L.setValue(dataBus.memRead(xxd));
                alu.SLI8(regs.L);
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // sli (xx+d)
            case 0x36:
                tStates += 23; {
                Register regval = new Register(dataBus.memRead(xxd));
                alu.SLI8(regval);
                dataBus.memWrite(xxd, (byte) regval.getValue());
            }
                break;

            // sli (xx+d),a
            case 0x37:
                tStates += 23;
                regs.A.setValue(dataBus.memRead(xxd));
                alu.SLI8(regs.A);
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // srl (xx+d),b
            case 0x38:
                tStates += 23;
                regs.B.setValue(dataBus.memRead(xxd));
                alu.SRL8(regs.B);
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // srl (xx+d),c
            case 0x39:
                tStates += 23;
                regs.C.setValue(dataBus.memRead(xxd));
                alu.SRL8(regs.C);
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // srl (xx+d),d
            case 0x3A:
                tStates += 23;
                regs.D.setValue(dataBus.memRead(xxd));
                alu.SRL8(regs.D);
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // srl (xx+d),e
            case 0x3B:
                tStates += 23;
                regs.E.setValue(dataBus.memRead(xxd));
                alu.SRL8(regs.E);
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // srl (xx+d),h
            case 0x3C:
                tStates += 23;
                regs.H.setValue(dataBus.memRead(xxd));
                alu.SRL8(regs.H);
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // srl (xx+d),l
            case 0x3D:
                tStates += 23;
                regs.L.setValue(dataBus.memRead(xxd));
                alu.SRL8(regs.L);
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // srl (xx+d)
            case 0x3E:
                tStates += 23; {
                Register regval = new Register(dataBus.memRead(xxd));
                alu.SRL8(regval);
                dataBus.memWrite(xxd, (byte) regval.getValue());
            }
                break;

            // srl (xx+d),a
            case 0x3F:
                tStates += 23;
                regs.A.setValue(dataBus.memRead(xxd));
                alu.SRL8(regs.A);
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // bit 0,(xx+d)
            case 0x40:
            case 0x41:
            case 0x42:
            case 0x43:
            case 0x44:
            case 0x45:
            case 0x46:
            case 0x47:
                tStates += 20;
                alu.BIT8(dataBus.memRead(xxd), 0);
                regs.setF5((xxd & 0x2000) != 0);
                regs.setF3((xxd & 0x0800) != 0);

                break;

            // bit 1,(xx+d)
            case 0x48:
            case 0x49:
            case 0x4A:
            case 0x4B:
            case 0x4C:
            case 0x4D:
            case 0x4E:
            case 0x4F:
                tStates += 20;
                alu.BIT8(dataBus.memRead(xxd), 1);
                regs.setF5((xxd & 0x2000) != 0);
                regs.setF3((xxd & 0x0800) != 0);
                break;

            // bit 2,(xx+d)
            case 0x50:
            case 0x51:
            case 0x52:
            case 0x53:
            case 0x54:
            case 0x55:
            case 0x56:
            case 0x57:
                tStates += 20;
                alu.BIT8(dataBus.memRead(xxd), 2);
                regs.setF5((xxd & 0x2000) != 0);
                regs.setF3((xxd & 0x0800) != 0);
                break;

            // bit 3,(xx+d)
            case 0x58:
            case 0x59:
            case 0x5A:
            case 0x5B:
            case 0x5C:
            case 0x5D:
            case 0x5E:
            case 0x5F:
                tStates += 20;
                alu.BIT8(dataBus.memRead(xxd), 3);
                regs.setF5((xxd & 0x2000) != 0);
                regs.setF3((xxd & 0x0800) != 0);
                break;

            // bit 4,(xx+d)
            case 0x60:
            case 0x61:
            case 0x62:
            case 0x63:
            case 0x64:
            case 0x65:
            case 0x66:
            case 0x67:
                tStates += 20;
                alu.BIT8(dataBus.memRead(xxd), 4);
                regs.setF5((xxd & 0x2000) != 0);
                regs.setF3((xxd & 0x0800) != 0);
                break;

            // bit 5,(xx+d)
            case 0x68:
            case 0x69:
            case 0x6A:
            case 0x6B:
            case 0x6C:
            case 0x6D:
            case 0x6E:
            case 0x6F:
                tStates += 20;
                alu.BIT8(dataBus.memRead(xxd), 5);
                regs.setF5((xxd & 0x2000) != 0);
                regs.setF3((xxd & 0x0800) != 0);
                break;

            // bit 6,(xx+d)
            case 0x70:
            case 0x71:
            case 0x72:
            case 0x73:
            case 0x74:
            case 0x75:
            case 0x76:
            case 0x77:
                tStates += 20;
                alu.BIT8(dataBus.memRead(xxd), 6);
                regs.setF5((xxd & 0x2000) != 0);
                regs.setF3((xxd & 0x0800) != 0);
                break;

            // bit 7,(xx+d)
            case 0x78:
            case 0x79:
            case 0x7A:
            case 0x7B:
            case 0x7C:
            case 0x7D:
            case 0x7E:
            case 0x7F:
                tStates += 20;
                alu.BIT8(dataBus.memRead(xxd), 7);
                regs.setF5((xxd & 0x2000) != 0);
                regs.setF3((xxd & 0x0800) != 0);
                break;

            // res 0,(xx+d),b
            case 0x80:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) & 0xFE));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // res 0,(xx+d),c
            case 0x81:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) & 0xFE));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // res 0,(xx+d),d
            case 0x82:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) & 0xFE));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // res 0,(xx+d),e
            case 0x83:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) & 0xFE));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // res 0,(xx+d),h
            case 0x84:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) & 0xFE));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // res 0,(xx+d),l
            case 0x85:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) & 0xFE));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // res 0,(xx+d)
            case 0x86:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) & 0xFE));
                break;

            // res 0,(xx+d),a
            case 0x87:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) & 0xFE));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // res 1,(xx+d),b
            case 0x88:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) & 0xFD));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // res 1,(xx+d),c
            case 0x89:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) & 0xFD));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // res 1,(xx+d),d
            case 0x8A:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) & 0xFD));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // res 1,(xx+d),e
            case 0x8B:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) & 0xFD));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // res 1,(xx+d),h
            case 0x8C:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) & 0xFD));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // res 1,(xx+d),l
            case 0x8D:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) & 0xFD));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // res 1,(xx+d)
            case 0x8E:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) & 0xFD));
                break;

            // res 1,(xx+d),a
            case 0x8F:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) & 0xFD));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // res 2,(xx+d),b
            case 0x90:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) & 0xFB));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // res 2,(xx+d),c
            case 0x91:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) & 0xFB));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // res 2,(xx+d),d
            case 0x92:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) & 0xFB));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // res 2,(xx+d),e
            case 0x93:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) & 0xFB));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // res 2,(xx+d),h
            case 0x94:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) & 0xFB));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // res 2,(xx+d),l
            case 0x95:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) & 0xFB));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // res 2,(xx+d)
            case 0x96:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) & 0xFB));
                break;

            // res 2,(xx+d),a
            case 0x97:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) & 0xFB));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // res 3,(xx+d),b
            case 0x98:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) & 0xF7));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // res 3,(xx+d),c
            case 0x99:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) & 0xF7));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // res 3,(xx+d),d
            case 0x9A:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) & 0xF7));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // res 3,(xx+d),e
            case 0x9B:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) & 0xF7));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // res 3,(xx+d),h
            case 0x9C:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) & 0xF7));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // res 3,(xx+d),l
            case 0x9D:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) & 0xF7));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // res 3,(xx+d)
            case 0x9E:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) & 0xF7));
                break;

            // res 3,(xx+d),a
            case 0x9F:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) & 0xF7));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // res 4,(xx+d),b
            case 0xA0:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) & 0xEF));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // res 4,(xx+d),c
            case 0xA1:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) & 0xEF));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // res 4,(xx+d),d
            case 0xA2:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) & 0xEF));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // res 4,(xx+d),e
            case 0xA3:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) & 0xEF));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // res 4,(xx+d),h
            case 0xA4:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) & 0xEF));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // res 4,(xx+d),l
            case 0xA5:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) & 0xEF));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // res 4,(xx+d)
            case 0xA6:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) & 0xEF));
                break;

            // res 4,(xx+d),a
            case 0xA7:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) & 0xEF));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // res 5,(xx+d),b
            case 0xA8:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) & 0xDF));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // res 5,(xx+d),c
            case 0xA9:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) & 0xDF));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // res 5,(xx+d),d
            case 0xAA:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) & 0xDF));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // res 5,(xx+d),e
            case 0xAB:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) & 0xDF));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // res 5,(xx+d),h
            case 0xAC:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) & 0xDF));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // res 5,(xx+d),l
            case 0xAD:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) & 0xDF));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // res 5,(xx+d)
            case 0xAE:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) & 0xDF));
                break;

            // res 5,(xx+d),a
            case 0xAF:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) & 0xDF));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // res 6,(xx+d),b
            case 0xB0:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) & 0xBF));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // res 6,(xx+d),c
            case 0xB1:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) & 0xBF));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // res 6,(xx+d),d
            case 0xB2:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) & 0xBF));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // res 6,(xx+d),e
            case 0xB3:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) & 0xBF));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // res 6,(xx+d),h
            case 0xB4:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) & 0xBF));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // res 6,(xx+d),l
            case 0xB5:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) & 0xBF));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // res 6,(xx+d)
            case 0xB6:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) & 0xBF));
                break;

            // res 6,(xx+d),a
            case 0xB7:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) & 0xBF));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // res 7,(xx+d),b
            case 0xB8:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) & 0x7F));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // res 7,(xx+d),c
            case 0xB9:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) & 0x7F));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // res 7,(xx+d),d
            case 0xBA:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) & 0x7F));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // res 7,(xx+d),e
            case 0xBB:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) & 0x7F));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // res 7,(xx+d),h
            case 0xBC:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) & 0x7F));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // res 7,(xx+d),l
            case 0xBD:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) & 0x7F));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // res 7,(xx+d)
            case 0xBE:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) & 0x7F));
                break;

            // res 7,(xx+d),a
            case 0xBF:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) & 0x7F));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // set 0,(xx+d),b
            case 0xC0:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) | 0x01));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // set 0,(xx+d),c
            case 0xC1:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) | 0x01));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // set 0,(xx+d),d
            case 0xC2:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) | 0x01));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // set 0,(xx+d),e
            case 0xC3:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) | 0x01));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // set 0,(xx+d),h
            case 0xC4:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) | 0x01));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // set 0,(xx+d),l
            case 0xC5:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) | 0x01));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // set 0,(xx+d)
            case 0xC6:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) | 0x01));
                break;

            // set 0,(xx+d),a
            case 0xC7:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) | 0x01));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // set 1,(xx+d),b
            case 0xC8:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) | 0x02));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // set 1,(xx+d),c
            case 0xC9:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) | 0x02));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // set 1,(xx+d),d
            case 0xCA:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) | 0x02));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // set 1,(xx+d),e
            case 0xCB:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) | 0x02));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // set 1,(xx+d),h
            case 0xCC:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) | 0x02));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // set 1,(xx+d),l
            case 0xCD:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) | 0x02));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // set 1,(xx+d)
            case 0xCE:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) | 0x02));
                break;

            // set 1,(xx+d),a
            case 0xCF:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) | 0x02));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // set 2,(xx+d),b
            case 0xD0:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) | 0x04));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // set 2,(xx+d),c
            case 0xD1:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) | 0x04));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // set 2,(xx+d),d
            case 0xD2:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) | 0x04));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // set 2,(xx+d),e
            case 0xD3:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) | 0x04));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // set 2,(xx+d),h
            case 0xD4:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) | 0x04));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // set 2,(xx+d),l
            case 0xD5:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) | 0x04));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // set 2,(xx+d)
            case 0xD6:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) | 0x04));
                break;

            // set 2,(xx+d),a
            case 0xD7:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) | 0x04));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // set 3,(xx+d),b
            case 0xD8:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) | 0x08));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // set 3,(xx+d),c
            case 0xD9:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) | 0x08));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // set 3,(xx+d),d
            case 0xDA:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) | 0x08));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // set 3,(xx+d),e
            case 0xDB:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) | 0x08));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // set 3,(xx+d),h
            case 0xDC:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) | 0x08));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // set 3,(xx+d),l
            case 0xDD:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) | 0x08));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // set 3,(xx+d)
            case 0xDE:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) | 0x08));
                break;

            // set 3,(xx+d),a
            case 0xDF:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) | 0x08));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // set 4,(xx+d),b
            case 0xE0:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) | 0x10));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // set 4,(xx+d),c
            case 0xE1:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) | 0x10));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // set 4,(xx+d),d
            case 0xE2:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) | 0x10));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // set 4,(xx+d),e
            case 0xE3:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) | 0x10));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // set 4,(xx+d),h
            case 0xE4:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) | 0x10));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // set 4,(xx+d),l
            case 0xE5:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) | 0x10));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // set 4,(xx+d)
            case 0xE6:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) | 0x10));
                break;

            // set 4,(xx+d),a
            case 0xE7:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) | 0x10));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // set 5,(xx+d),b
            case 0xE8:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) | 0x20));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // set 5,(xx+d),c
            case 0xE9:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) | 0x20));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // set 5,(xx+d),d
            case 0xEA:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) | 0x20));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // set 5,(xx+d),e
            case 0xEB:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) | 0x20));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // set 5,(xx+d),h
            case 0xEC:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) | 0x20));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // set 5,(xx+d),l
            case 0xED:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) | 0x20));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // set 5,(xx+d)
            case 0xEE:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) | 0x20));
                break;

            // set 5,(xx+d),a
            case 0xEF:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) | 0x20));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // set 6,(xx+d),b
            case 0xF0:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) | 0x40));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // set 6,(xx+d),c
            case 0xF1:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) | 0x40));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // set 6,(xx+d),d
            case 0xF2:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) | 0x40));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // set 6,(xx+d),e
            case 0xF3:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) | 0x40));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // set 6,(xx+d),h
            case 0xF4:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) | 0x40));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // set 6,(xx+d),l
            case 0xF5:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) | 0x40));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // set 6,(xx+d)
            case 0xF6:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) | 0x40));
                break;

            // set 6,(xx+d),a
            case 0xF7:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) | 0x40));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

            // set 7,(xx+d),b
            case 0xF8:
                tStates += 23;
                regs.B.setValue((byte) (dataBus.memRead(xxd) | 0x80));
                dataBus.memWrite(xxd, (byte) regs.B.getValue());
                break;

            // set 7,(xx+d),c
            case 0xF9:
                tStates += 23;
                regs.C.setValue((byte) (dataBus.memRead(xxd) | 0x80));
                dataBus.memWrite(xxd, (byte) regs.C.getValue());
                break;

            // set 7,(xx+d),d
            case 0xFA:
                tStates += 23;
                regs.D.setValue((byte) (dataBus.memRead(xxd) | 0x80));
                dataBus.memWrite(xxd, (byte) regs.D.getValue());
                break;

            // set 7,(xx+d),e
            case 0xFB:
                tStates += 23;
                regs.E.setValue((byte) (dataBus.memRead(xxd) | 0x80));
                dataBus.memWrite(xxd, (byte) regs.E.getValue());
                break;

            // set 7,(xx+d),h
            case 0xFC:
                tStates += 23;
                regs.H.setValue((byte) (dataBus.memRead(xxd) | 0x80));
                dataBus.memWrite(xxd, (byte) regs.H.getValue());
                break;

            // set 7,(xx+d),l
            case 0xFD:
                tStates += 23;
                regs.L.setValue((byte) (dataBus.memRead(xxd) | 0x80));
                dataBus.memWrite(xxd, (byte) regs.L.getValue());
                break;

            // set 7,(xx+d)
            case 0xFE:
                tStates += 23;
                dataBus.memWrite(xxd, (byte) (dataBus.memRead(xxd) | 0x80));
                break;

            // set 7,(xx+d),a
            case 0xFF:
                tStates += 23;
                regs.A.setValue((byte) (dataBus.memRead(xxd) | 0x80));
                dataBus.memWrite(xxd, (byte) regs.A.getValue());
                break;

        }

    }


    /////////////////////////////////////////////////////////////////////////
    // INT():
    // If allowed by the iff1A flipflip, service a maskable interrupt.
    // Versión simplificada del INT
    // https://codersbucket.blogspot.com/2015/04/interrupts-on-zx-spectrum-what-are.html
    // https://retrocomputing.stackexchange.com/a/20096

    public void INT() {
        if (regs.getiff1A()) {
            // If over a HALT instruction, increment PC
            if (dataBus.memRead(regs.getPC()) == 0x76)
                regs.setPC((short) (regs.getPC() + 1));

            regs.setiff1A(false);
            regs.setiff1B(false);
            push16(regs.getPC());

            switch (regs.getIM()) {
                case 0x00: // Mode 0: Interrupción hardware, ejecuta instrucción del bus de datos,
                           // simplificada para el Spectrum (lee 0xFF = RST 38h)
                    tStates += 13;
                    regs.setPC((short) 0x0038);
                    regs.setWZ((short) 0x0038);
                    regs.setQF(false);
                    break;

                case 0x01: // Mode 1: Ejecuta rst 38h
                    tStates += 13;
                    regs.setPC((short) 0x0038);
                    regs.setWZ((short) 0x0038);
                    regs.setQF(false);
                    break;

                case 0x02: // Mode 2: Salta a la dirección contenida en registro I + bus de datos. Asumimos
                           // en el bus de datos FF al no existir hardware externo
                    tStates += 19;
                    regs.setPC(read16((short) (((byte) regs.I.getValue() << 8) | 0xFF)));
                    regs.setWZ(regs.getPC());
                    regs.setQF(false);
                    break;
            }

            REFRESH_CYCLE();

 
        }
    }

    // NMI():
    // Service a non-maskable interrupt.

    public void NMI() {
        tStates += 15;

        regs.setiff1B(regs.getiff1A());
        regs.setiff1A(false);

        // If over a HALT instruction, increment PC
        if (dataBus.memRead(regs.getPC()) == 0x76)
            regs.setPC((short) (regs.getPC() + 1));

        push16(regs.getPC());
        regs.setPC((short) 0x0066);
        REFRESH_CYCLE();
    }

    // Reset basado en la definición de reset Z80, ni mas ni menos
    public void RESET()
    {
        // Inicializar el contador de tStates
        tStates = 0;

        // resets the interrupt enable flip-flop
        regs.setiff1A(false);
        regs.setiff1B(false);
        // clears the Program Counter and registers I and R
        regs.setPC((short) 0);
        regs.IR.setValue((short) 0);
        // sets the interrupt status to Mode 0
        regs.setIM((byte) 0);



    }



    //////////////////////////////////////////////////
    // Getters y setters para el bus de datos (memoria e I/O)
    //////////////////////////////////////////////////

    public void setDataBus(Z80Bus dataBus) {
        this.dataBus = dataBus;
    }

    public Z80Bus getDataBus() {
        return dataBus;
    }

    //////////////////////////////////////////////////
    // Getters y setters tStates
    //////////////////////////////////////////////////

    public void setTStates(int tStates) {
        this.tStates = tStates;
    }

    public int getTStates() {
        return tStates;
    }

    //////////////////////////////////////////////////
    // Getters y setters registros para depuración
    //////////////////////////////////////////////////

    public Z80Registers getRegisters() {
        return regs;
    }

    public void setRegisters(Z80Registers regs) {
        this.regs = regs;
    }


    ////////////////////////////////////////////////////////////
    // Métodos privados de utilidad generales
    ////////////////////////////////////////////////////////////

    // Leer un valor de 16 bits desde la dirección 'addr'
    private short read16(short addr) {
        return (short) ((dataBus.memRead(addr) & 0x00FF) | (dataBus.memRead((short) (addr + 1)) << 8));
    }

    // Escribir un valor de 16 bits en la dirección 'addr'
    private void write16(short addr, short value) {
        dataBus.memWrite(addr, (byte) (value & 0xFF));
        dataBus.memWrite((short) (addr + 1), (byte) (value >> 8));
    }

    // Hacer un push de un valor de 16 bits en la pila (stack)
    private void push16(short value) {
        regs.setSP((short) (regs.getSP() - 2));
        write16(regs.getSP(), value);
    }

    // Hacer un pop de un valor de 16 bits de la pila
    private short pop16() {
        short retVal = read16(regs.getSP());
        regs.setSP((short) (regs.getSP() + 2));
        return retVal;
    }

    ////////////////////////////////////////////////////////////////////
    // Métodos privados asociados a operaciones de ciertas instrucciones
    ///////////////////////////////////////////////////////////////////

    // Operacion in <reg>, (C)
    private void IN8(Register regval, short port) {
        regs.setWZ((short) (port + 1));
        byte val = dataBus.ioRead(port);
        regval.setValue(val);
        regs.setSF((val & 0x80) != 0);
        regs.setZF(val == 0);
        regs.setHF(false);
        regs.setPF(parityTable[val & 0xFF]);
        regs.setNF(false);
        regs.setF3((port & 0x0800) != 0);
        regs.setF5((port & 0x2000) != 0);
    }

    // Operacion LDI/LDIR/LDD/LDDR
    private void LD_BLOCK(boolean increment) {
        // increment == true -> LDI* ; == false -> LDD*
        byte val = dataBus.memRead(regs.HL.getValue16());
        dataBus.memWrite(regs.DE.getValue16(), val);
        regs.BC.setValue((short) (regs.BC.getValue16() - 1));
        if (increment) {
            regs.DE.setValue((short) (regs.DE.getValue16() + 1));
            regs.HL.setValue((short) (regs.HL.getValue16() + 1));
        } else {
            regs.DE.setValue((short) (regs.DE.getValue16() - 1));
            regs.HL.setValue((short) (regs.HL.getValue16() - 1));
        }
        regs.setPF(regs.BC.getValue16() != 0);
        regs.setHF(false);
        regs.setNF(false);
        byte aux = (byte) (val + regs.A.getValue());
        regs.setF3((aux & 0x08) != 0);
        regs.setF5((aux & 0x02) != 0);
    }

    //Operacion OUTI/OTIR/OUTD/OTDR
    private void OUT_BLOCK(boolean increment) {
        // increment == true -> OUTI/OTIR ; == false -> OUTD/OTDR
        byte aux = (byte) (regs.B.getValue() - 1);
        regs.B.setValue(aux);
        byte data = dataBus.memRead(regs.HL.getValue16());
        dataBus.ioWrite(regs.BC.getValue16(), data);
        if (increment) {
            regs.HL.setValue((short) (regs.HL.getValue16() + 1));
            regs.setWZ((short) (regs.BC.getValue16() + 1));
        } else {
            regs.HL.setValue((short) (regs.HL.getValue16() - 1));
            regs.setWZ((short) (regs.BC.getValue16() - 1));
        }
        int testData = (data & 0xFF) + (regs.L.getValue() & 0xFF);
        regs.setSF((aux & 0x80) != 0);
        regs.setZF(aux == 0);
        regs.setNF((data & 0x80) != 0);
        regs.setCF(testData > 255);
        regs.setHF(testData > 255);
        regs.setPF(parityTable[((testData & 0x07) ^ aux) & 0xFF]);
        regs.setF3((aux & 0x08) != 0);
        regs.setF5((aux & 0x20) != 0);
    }

    // Operacion INI/INIR/IND/INDR
    private void IN_BLOCK(boolean increment) {
        // increment == true -> INI* ; == false -> IND*
        byte data = dataBus.ioRead(regs.BC.getValue16());
        dataBus.memWrite(regs.HL.getValue16(), data);
        byte aux = (byte) (regs.B.getValue() - 1);
        int testData;
        if (increment) {
            regs.HL.setValue((short) (regs.HL.getValue16() + 1));
            regs.setWZ((short) (regs.BC.getValue16() + 1));
            testData = (data & 0xFF) + ((regs.C.getValue() + 1) & 0xFF);
        } else {
            regs.HL.setValue((short) (regs.HL.getValue16() - 1));
            regs.setWZ((short) (regs.BC.getValue16() - 1));
            testData = (data & 0xFF) + ((regs.C.getValue() - 1) & 0xFF);
        }
        regs.B.setValue(aux);
        regs.setSF((aux & 0x80) != 0);
        regs.setZF(aux == 0);
        regs.setNF((data & 0x80) != 0);
        regs.setCF(testData > 255);
        regs.setHF(testData > 255);
        regs.setPF(parityTable[((testData & 0x07) ^ aux) & 0xFF]);
        regs.setF3((aux & 0x08) != 0);
        regs.setF5((aux & 0x20) != 0);
    }

    // Operacion flags INIR/INDR/OTIR/OTDR
    private void INxROUTxRFlags_BLOCK() {
        regs.setF3((regs.getPC() & 0x0800) != 0);
        regs.setF5((regs.getPC() & 0x2000) != 0);
        if (regs.getCF()) {
            regs.setPF(!(regs.getPF() ^ parityTable[(((regs.B.getValue() & 0xff) + (regs.getNF() ? -1 : 1)) & 0x07)]));
            regs.setHF((regs.B.getValue() & 0x0f) == (regs.getNF() ? 0x00 : 0x0F));
        } else {
            regs.setPF(!(regs.getPF() ^ parityTable[((regs.B.getValue() & 0xff) & 0x07)]));
            regs.setHF(false);
        }
    }


    ////////////////////////////
    // Gestión registro de refresco R
    // Solo hay ciclos de refresco
    // - sin prefijo
    // - al obtener los datos de prefijo
    //
    // https://stackoverflow.com/questions/8540518/z80-memory-refresh-register
    // Ver los comenarios hasta el final, la respuesta más aceptada no parece la
    //////////////////////////// correcta
    //
    // http://www.z80.info/z80undoc3.txt
    // Hay un apartado para el registro R

    private void REFRESH_CYCLE() {
        byte val = (byte) ((regs.R.getValue() + 1) & 0x7F);
        regs.R.setValue((byte) ((regs.R.getValue() & 0x80) | val));
    }

    // Para el caso de tener que reevaluar un codigo de instrucción en los prefijos
    private void REFRESH_CYCLE_BACK() {
        byte val = (byte) ((regs.R.getValue() - 1) & 0x7F);
        regs.R.setValue((byte) ((regs.R.getValue() & 0x80) | val));
    }


}
