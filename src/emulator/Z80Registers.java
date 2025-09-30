public class Z80Registers {

    // Registros de 8 bits encapsulados en objetos Register
    // Excepto el par AF, el resto se pueden manejar como 8 o 16 bits
    public Register A = new Register((byte) 0);
    private byte F = 0; // Registro F, gestionado a través de los flags, por eso es privado

    public Register B = new Register((byte) 0);
    public Register C = new Register((byte) 0);
    public Register D = new Register((byte) 0);
    public Register E = new Register((byte) 0);
    public Register H = new Register((byte) 0);
    public Register L = new Register((byte) 0);

    // Pares de registros de 16 bits (encapsulamos el uso de los registros de 8
    // bits)
    public Register BC = new Register(this.B, this.C); // BC combina B y C
    public Register DE = new Register(this.D, this.E); // DE combina D y E
    public Register HL = new Register(this.H, this.L); // HL combina H y L

    // Alternate registers -> son puros de 16 bits porque no se opera con ellos
    public Register altAF = new Register((short) 0);
    public Register altBC = new Register((short) 0);
    public Register altDE = new Register((short) 0);
    public Register altHL = new Register((short) 0);

    // Punteros de programa y stack (16 bits)
    private short SP; // Stack Pointer de 16 bits
    private short PC; // PC de 16 bits

    // Otros registros especiales
    public Register IX = new Register((short) 0); // Index Register IX
    public Register IY = new Register((short) 0); // Index Register IY
    // Interrupt Page Address Register
    public Register I = new Register((byte) 0);
    public Register R = new Register((byte) 0); // Este registro se debe de poder manejar de forma individual
    public Register IR = new Register(this.I, this.R); // Interrupt Page Address Register (16 bits)

    // Registro ficticio para la ejecución de instrucciones de los registros IX, IY
    // Tiene que permitir manejar ambos bytes del registro, hay que crearlo como un
    // par de registros de 8 bits
    public Register HX = new Register((byte) 0); // MSB
    public Register LX = new Register((byte) 0); // LSB
    public Register XX = new Register(this.HX, this.LX); // // Aux Register for IX, IY instructions, joins HX & LX

    // Registro de 16 bits interno tambien denominado MEMPTR
    private short WZ; // Almacenamiento temporal en operaciones

    // Flags
    private boolean SF; // Sign Flag
    private boolean ZF; // Zero Flag
    private boolean F5; // Bit 5 de flags
    private boolean HF; // Half-carry Flag
    private boolean F3; // Bit 3 de flags
    private boolean PF; // Parity/Overflow Flag
    private boolean NF; // Add/Subtract Flag
    private boolean CF; // Carry Flag

    // Flag Q, utilizado para 'ciertas gestiones', y su asociado lastQF
    private boolean QF = false;
    private boolean lastQF = false;

    // Interrupt Flip Flops
    private boolean iff1A; // Flip flop A
    private boolean iff1B; // Flip flop B

    // Interrupt mode
    private byte IM;

    // Métodos de acceso directo para AF
    public short getAF() {
        return (short) ((((byte) A.getValue()) << 8) | (F & 0xFF));
    }

    public void setAF(short value) {
        A.setValue((byte) ((value >> 8) & 0xFF));
        F = (byte) (value & 0xFF);
        updateFlagsFromF();
    }

    // Gestión de los flags como bits en el registro F
    private void updateFlagsFromF() {
        // Actualiza los flags individuales a partir de los bits de F
        SF = (F & 0x80) != 0;
        ZF = (F & 0x40) != 0;
        F5 = (F & 0x20) != 0;
        HF = (F & 0x10) != 0;
        F3 = (F & 0x08) != 0;
        PF = (F & 0x04) != 0;
        NF = (F & 0x02) != 0;
        CF = (F & 0x01) != 0;
    }

    private void updateFfromFlags() {
        // Actualiza el valor de F a partir de los flags individuales
        F = 0;
        if (SF)
            F |= 0x80;
        if (ZF)
            F |= 0x40;
        if (F5)
            F |= 0x20;
        if (HF)
            F |= 0x10;
        if (F3)
            F |= 0x08;
        if (PF)
            F |= 0x04;
        if (NF)
            F |= 0x02;
        if (CF)
            F |= 0x01;
        // Cualquier cambio en un flag activa QF
        QF = true;
    }

    // Métodos para trabajar con flags individuales (getters y setters)
    public boolean getSF() {
        return SF;
    }

    public void setSF(boolean sf) {
        this.SF = sf;
        updateFfromFlags();
    }

    public boolean getZF() {
        return ZF;
    }

    public void setZF(boolean zf) {
        this.ZF = zf;
        updateFfromFlags();
    }

    public boolean getF5() {
        return F5;
    }

    public void setF5(boolean f5) {
        this.F5 = f5;
        updateFfromFlags();
    }

    public boolean getHF() {
        return HF;
    }

    public void setHF(boolean hf) {
        this.HF = hf;
        updateFfromFlags();
    }

    public boolean getF3() {
        return F3;
    }

    public void setF3(boolean f3) {
        this.F3 = f3;
        updateFfromFlags();
    }

    public boolean getPF() {
        return PF;
    }

    public void setPF(boolean pf) {
        this.PF = pf;
        updateFfromFlags();
    }

    public boolean getNF() {
        return NF;
    }

    public void setNF(boolean nf) {
        this.NF = nf;
        updateFfromFlags();
    }

    public boolean getCF() {
        return CF;
    }

    public void setCF(boolean cf) {
        this.CF = cf;
        updateFfromFlags();
    }

    // Acceso al pseudo flag Q
    public boolean getQF() {
        return QF;
    }

    public boolean getLastQF() {
        return lastQF;
    }

    public void preserveQF() {
        lastQF = QF;
    }

    public void setQF(boolean value) {
        QF = value;
    }

    // Métodos para trabajar con flags individuales (getters y setters)
    public boolean getiff1A() {
        return iff1A;
    }

    public void setiff1A(boolean iff) {
        this.iff1A = iff;
    }

    public boolean getiff1B() {
        return iff1B;
    }

    public void setiff1B(boolean iff) {
        this.iff1B = iff;
    }

    // Acceso y modificacion del interrupt mode
    public byte getIM() {
        return IM;
    }

    public void setIM(byte mode) {
        this.IM = mode;
    }

    // Acceso a PC y SP
    public short getPC() {
        return PC;
    }

    public void setPC(short value) {
        PC = value;
    }

    public short getSP() {
        return SP;
    }

    public void setSP(short value) {
        SP = value;
    }

    // Acceso al registro temporal interno de 16 bits
    // Lo gestionamos como in registro especial. Incluye acceso a parte alta y baja
    // del registro
    public short getWZ() {
        return WZ;
    }

    public byte getW() {
        return (byte) ((WZ >> 8) & 0xff);
    }

    public byte getZ() {
        return (byte) (WZ & 0xff);
    }

    public void setWZ(short value) {
        WZ = value;
    }

    public void setW(byte value) {
        WZ = (short) (((value << 8) & 0xff00) | (WZ & 0xff));
    }

    public void setZ(byte value) {
        WZ = (short) ((value & 0xff) | (WZ & 0xff00));
    }

    // El reset hay que repasarlo con la nueva estructura
    public void reset() {
        // Registros de 8 bits
        A.setValue((byte) 0xFF);
        F = (byte) 0xFF;
        B.setValue((byte) 0);
        C.setValue((byte) 0);
        D.setValue((byte) 0);
        E.setValue((byte) 0);
        H.setValue((byte) 0);
        L.setValue((byte) 0);

        // Registros alternativos
        altAF.setValue((short) 0);
        altBC.setValue((short) 0);
        altDE.setValue((short) 0);
        altHL.setValue((short) 0);

        // Registros de 16 bits
        PC = 0;
        SP = (short) 0xFFFF;
        IX.setValue((short) 0);
        IY.setValue((short) 0);
        IR.setValue((short) 0);

        // Registro AF (F es manejado por flags)

        // Resetear los flags a true (es su valor en el reset
        SF = true;
        ZF = true;
        HF = true;
        PF = true;
        NF = true;
        CF = true;

        // Actualizar el registro F para reflejar los flags reseteados
        updateFfromFlags();

        // Resetear flip flop interrupciones
        iff1A = false;
        iff1B = false;

        // Resetear interrupt mode
        IM = 0;

        // Registro WZ
        WZ = 0;

        // flag Q y asociado
        QF = false;
        lastQF = false;

    }

}
