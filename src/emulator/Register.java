public class Register {

	private boolean r8bits; //indicador de registro de 8 bits
	private boolean r16bitsPure; //indicador de registro de 16 bits puro (no se puede acceder como un registro de 8 bits)
	private byte value;  // Para registros de 8 bits
	private short value16;  // Para registros de 8 bits
	private Register highReg, lowReg; //Par de registros de 8 bits para los de 16 bits

	//Creamos registro de 8 bits
    public Register(byte initialValue) {  
        this.value = initialValue;
		this.r8bits = true;
		this.r16bitsPure = false;
    }

	//Creamos registro de 16 bits con las referencias a sus registros de 8 bits
    public Register(Register high, Register low) {  
        this.highReg = high;
        this.lowReg = low;
		this.r8bits = false;
		this.r16bitsPure = false;
    }

	//Creamos registro de 16 bits puro
    public Register(short initialValue) {  
        this.value16 = initialValue;
		this.r8bits = false;
		this.r16bitsPure = true;
    }


	// Accesos al valor
    public byte getValue() {
		if (r8bits) {
			return value;
		}
		else 
		{
			System.out.printf( "AVISO getValue 16 bits en 8 bits\n"); 
			return value;
		}

    }

    public short getValue16() {
		if (r8bits) {
			System.out.printf( "AVISO getValue 8 bits en 16 bits\n"); 
			return value;
		}
		else if (r16bitsPure) {
			return value16;
		}
		else{
			return (this.combineBytes((byte)this.highReg.getValue(), (byte)this.lowReg.getValue()));
		}
    }


	// Asignacion de valor
    public void setValue(byte value) {  //actualizar 8 bits
        this.value = value;
    } 
 
    public void setValue(short value) {  //actualiza los de 8 bits pero si es puro 16 bits no tiene registros de 8 bits
		if (r16bitsPure) {
			this.value16 = value;
		}
		else{
			this.highReg.setValue( highByte(value));
			this.lowReg.setValue( lowByte(value));
		}
    }


    // Para registros de 16 bits, usaremos un método especial
    public static short combineBytes(byte high, byte low) {
        return (short) ((high << 8) | (low & 0xFF));
    }

    public static byte highByte(short value) {
        return (byte) (value >> 8);
    }

    public static byte lowByte(short value) {
        return (byte) (value & 0xFF);
    }
}
