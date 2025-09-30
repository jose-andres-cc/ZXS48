public class Z80ALU {

    private Z80Registers regs;
    private boolean[] parityTable;

    public Z80ALU(Z80Registers regs, boolean[] parityTable) {
        this.regs = regs;
        this.parityTable = parityTable;
    }

    public void INC_R8(Register reg) {
        byte value = (byte)reg.getValue();
        byte result = (byte) (value + 1);
        reg.setValue(result);
        regs.setZF(result == 0);
        regs.setSF((result & 0x80) != 0);
        regs.setHF((value & 0x0F) + 1 > 0x0F);
		regs.setPF(result == (byte)0x80);
        regs.setNF(false);
		regs.setF3((result & 0x008) != 0);
		regs.setF5((result & 0x020) != 0);
    }

    public void DEC_R8(Register reg) {
        byte value = (byte)reg.getValue();
        byte result = (byte) (value - 1);
        reg.setValue(result);
        regs.setZF(result == 0);
        regs.setSF((result & 0x80) != 0);
        regs.setHF((value & 0x0F) < 1);
		regs.setPF(result == 0x7f);
        regs.setNF(true);
		regs.setF3((result & 0x08) != 0);
		regs.setF5((result & 0x20) != 0);
    }

    public void ADD_R16(Register reg1, short reg2value) {
		regs.setWZ((short) (reg1.getValue16() + 1));
        int value1 = reg1.getValue16() & 0xFFFF;
        int value2 = reg2value & 0xFFFF;
        int result = value1 + value2;
        reg1.setValue((short) result);
        regs.setHF(((value1 & 0x0FFF) + (value2 & 0x0FFF)) > 0x0FFF);
        regs.setCF(result > 0xFFFF);
        regs.setNF(false);
		regs.setF3((result & 0x0800) != 0);
		regs.setF5((result & 0x2000) != 0);
    }


	public void SBC_R16(short v) {
		regs.setWZ((short)(regs.HL.getValue16() + 1));
		int aux = (regs.HL.getValue16() - v - (regs.getCF() ? 1 : 0));
		regs.setSF((aux & 0x8000) != 0);
		regs.setZF(((aux & 0xFFFF) == 0));
		regs.setHF(((aux ^ regs.HL.getValue16() ^ v) & 0x1000) != 0);
		regs.setPF(((regs.HL.getValue16() ^ v) & (regs.HL.getValue16() ^ aux) & 0x8000 ) != 0);
		regs.setNF(true);
		regs.setCF((regs.HL.getValue16()& 0xffff) < (v & 0xffff) + (regs.getCF() ? 1 : 0));
		regs.setF3((aux & 0x0800) != 0);
		regs.setF5((aux & 0x2000) != 0);
		regs.HL.setValue((short) aux);
	}

	public void SUB_R8(byte v) {
		int aux = (regs.A.getValue() & 0xFF) - (v & 0xFF);
		regs.setSF((aux & 0x80) != 0);
		regs.setZF(((aux & 0xFF) == 0));
		regs.setHF((aux & 0x0f) > (regs.A.getValue() & 0x0f));
		regs.setPF(((regs.A.getValue() ^ v) & (regs.A.getValue() ^ (byte)aux) & 0x80) !=0);
		regs.setNF(true);
		regs.setCF((aux & 0x100) != 0);
		regs.setF3((aux & 0x008) != 0);
		regs.setF5((aux & 0x020) != 0);
		regs.A.setValue((byte) aux);
	}
	
	public void ADD_R8(byte v) {
		int aux = (regs.A.getValue() & 0xFF) + (v & 0xFF);
		regs.setSF((aux & 0x80) != 0);
		regs.setZF((aux & 0xFF) == 0);
		regs.setHF((aux & 0x0f) < (regs.A.getValue() & 0x0f));
		regs.setPF((~(regs.A.getValue() ^ v) & (v ^ aux) & 0x80 )!= 0);
		regs.setNF(false);
		regs.setCF((aux & 0x100) != 0);
		regs.setF3((aux & 0x008) != 0);
		regs.setF5((aux & 0x020) != 0);
		regs.A.setValue((byte) aux);
	}

	public void ADC_R16(short v) {
		regs.setWZ((short)(regs.HL.getValue16()+1));
		int aux = (regs.HL.getValue16() & 0xffff) + (v & 0xffff) + (regs.getCF() ? 1 : 0);
		regs.setSF((aux & 0x8000) != 0);
		regs.setZF((aux & 0xffff) == 0);
		regs.setHF(((aux ^ regs.HL.getValue16() ^ v) & 0x1000) != 0);
		regs.setPF(((regs.HL.getValue16() ^ ~v) & (regs.HL.getValue16() ^ aux) & 0x8000) != 0);
		regs.setNF(false);
		regs.setCF(aux > 0xffff);
		regs.setF3((aux & 0x0800) != 0);
		regs.setF5((aux & 0x2000) != 0);
		regs.HL.setValue((short) aux);
	}


	public void ADC_R8(byte v) {
		int aux = (regs.A.getValue() & 0xFF) + (v & 0xFF) + (regs.getCF() ? 1 : 0);
		regs.setSF((aux & 0x80) != 0);
		regs.setZF((aux & 0xff) == 0);
		regs.setHF(((regs.A.getValue() ^ v ^ aux) & 0x10) != 0);
		regs.setPF((~(regs.A.getValue() ^ v) & (v ^ aux) & 0x80 )!= 0);
		regs.setNF(false);
		regs.setCF((aux & 0x100) != 0);
		regs.setF3((aux & 0x008) != 0);
		regs.setF5((aux & 0x020) != 0);
		regs.A.setValue((byte) aux);
	}


	public void AND_R8(byte v) {
		regs.A.setValue((byte) (regs.A.getValue() & v));  
		regs.setSF((regs.A.getValue() & 0x80) != 0);  
		regs.setZF(regs.A.getValue() == 0);  
		regs.setHF(true);  
		regs.setPF(parityTable[regs.A.getValue() & 0xFF]);  
		regs.setNF(false);  
		regs.setCF(false);  
		regs.setF3((regs.A.getValue() & 0x008) != 0);
		regs.setF5((regs.A.getValue() & 0x020) != 0);
	}
		

	
	public void BIT8(byte val, int bit) {  
		byte mask = (byte) (1 << bit);
		boolean result = (val & mask) == 0;
		regs.setZF(result);
		regs.setPF(result);
		regs.setSF((val & mask & 0x80) != 0);
		regs.setHF(true);
		regs.setNF(false);
		regs.setF5((val & 0x20) != 0);
		regs.setF3((val & 0x08) != 0);
		
	}

	public void BIT8_WZ_(byte val, int bit) {
		byte mask = (byte) (1 << bit);
		boolean result = (val & mask) == 0;
		regs.setZF(result);
		regs.setPF(result);
		regs.setSF((val & mask & 0x80) != 0);
		regs.setHF(true);
		regs.setNF(false);
		regs.setF5((regs.getWZ() & 0x2000) != 0);
		regs.setF3((regs.getWZ() & 0x0800) != 0);
		
	}
		
		
	public void CMP_R8(byte v) {
		int aux = (regs.A.getValue() & 0xFF) - (v & 0xFF);
		regs.setSF((aux & 0x80) != 0);
		regs.setCF((aux & 0x100) != 0);
		regs.setZF((aux & 0xFF) == 0);
		regs.setHF((aux & 0x0f) > (regs.A.getValue() & 0x0f));
		regs.setPF(((regs.A.getValue() ^ v) & (regs.A.getValue() ^ (byte)aux) & 0x80) !=0);
		regs.setNF(true);
		regs.setF3((v & 0x008) != 0);
		regs.setF5((v & 0x020) != 0);
	}
	

	public void CMP_R8_NOFLAGS(byte r, byte v) {
		int aux = (r & 0xFF) - (v & 0xFF);
		regs.setSF((aux & 0x80) != 0);
		regs.setZF((aux & 0xFF) == 0);
		regs.setHF((aux & 0x0f) > (r & 0x0f));
		regs.setNF(true);
	}


	public void OR_R8(byte v) {
		regs.A.setValue((byte) (regs.A.getValue() | v));
		regs.setSF((regs.A.getValue() & 0x80) != 0);
		regs.setZF((regs.A.getValue() == 0));
		regs.setHF(false);
		regs.setNF(false);
		regs.setCF(false);
		regs.setPF(parityTable[regs.A.getValue() & 0xFF]);
		regs.setF3((regs.A.getValue() & 0x008) != 0);
		regs.setF5((regs.A.getValue() & 0x020) != 0);
	}


	public void SBC_R8(byte v) {
		byte r = (byte)regs.A.getValue();
		int aux = (r & 0xFF) - (v & 0xFF) - (regs.getCF() ? 1 : 0);
		regs.setSF((aux & 0x80) != 0);
		regs.setZF((aux & 0xFF) == 0);
		regs.setHF(((regs.A.getValue() ^ v ^ aux) & 0x10) != 0);
		regs.setPF(((regs.A.getValue() ^ v) & (regs.A.getValue() ^ (byte)aux) & 0x80) !=0);
		regs.setNF(true);
		regs.setCF((aux & 0x100) != 0);
		regs.setF3((aux & 0x008) != 0);
		regs.setF5((aux & 0x020) != 0);
		regs.A.setValue((byte) aux);
	}


	public void XOR_R8(byte v) {
		regs.A.setValue( (byte) (regs.A.getValue() ^ v) );
		regs.setSF((regs.A.getValue() & 0x80) != 0);
		regs.setZF(regs.A.getValue() == 0);
		regs.setHF(false);
		regs.setNF(false);
		regs.setCF(false);
		regs.setPF(parityTable[regs.A.getValue() & 0xFF]);
		regs.setF3((regs.A.getValue() & 0x008) != 0);
		regs.setF5((regs.A.getValue() & 0x020) != 0);
	}


/////////////////////////////////////////////////////////////////////////
// Rotation operations


	public void RL8(Register reg) {
		byte val = (byte)reg.getValue();
		boolean cf = (val & 0x80) != 0;
		val = (byte) (val << 1);
		val |= (regs.getCF() ? 0x01 : 0x00);
		reg.setValue(val);
		regs.setSF((val & 0x80) != 0);
		regs.setZF(val == 0);
		regs.setHF(false);
		regs.setNF(false);
		regs.setPF(parityTable[val & 0xFF]);
		regs.setCF(cf);
		regs.setF3((val & 0x008) != 0);
		regs.setF5((val & 0x020) != 0);
	}

	public void RLC8(Register reg) {
		byte val = (byte)reg.getValue();
		val = (byte) ((val << 1) | ((val & 0x80) != 0 ? 0x01 : 0x00));
		reg.setValue(val);
		regs.setSF((val & 0x80) != 0);
		regs.setZF(val == 0);
		regs.setHF(false);
		regs.setNF(false);
		regs.setPF(parityTable[val & 0xFF]);
		regs.setCF((val & 0x01) != 0);
		regs.setF3((val & 0x008) != 0);
		regs.setF5((val & 0x020) != 0);
	}

	public void RR8(Register reg) {
		byte val = (byte)reg.getValue();
		boolean cf = (val & 0x01) != 0;
		val = (byte) ((val >> 1) & 0x7F);
		val |= (regs.getCF() ? 0x80 : 0x00);
		reg.setValue(val);
		regs.setSF((val & 0x80) != 0);
		regs.setZF(val == 0);
		regs.setHF(false);
		regs.setNF(false);
		regs.setPF(parityTable[val & 0xFF]);
		regs.setCF(cf);
		regs.setF3((val & 0x008) != 0);
		regs.setF5((val & 0x020) != 0);
	}

	public void RRC8(Register reg) {
		byte val = (byte)reg.getValue();
		regs.setCF((val & 0x01) != 0);
		val = (byte) ((val >> 1) & 0x7F);
		val |= regs.getCF() ? 0x80 : 0x00;
		reg.setValue(val);
		regs.setSF((val & 0x80) != 0);
		regs.setZF(val == 0);
		regs.setHF(false);
		regs.setNF(false);
		regs.setPF(parityTable[val & 0xFF]);
		regs.setF3((val & 0x008) != 0);
		regs.setF5((val & 0x020) != 0);
	}


	public void SLA8(Register reg) {
		byte val = (byte)reg.getValue();
		regs.setCF((val & 0x80) != 0);
		val = (byte) (val << 1);
		reg.setValue(val);
		regs.setSF((val & 0x80) != 0);
		regs.setZF(val == 0);
		regs.setHF(false);
		regs.setNF(false);
		regs.setPF(parityTable[val & 0xFF]);
		regs.setF3((val & 0x008) != 0);
		regs.setF5((val & 0x020) != 0);
	}


	public void SRA8(Register reg) {
		byte val = (byte)reg.getValue();
		regs.setCF((val & 0x01) != 0);
		val = (byte) ((val & 0x80) | (val >> 1));
		reg.setValue(val);
		regs.setSF((val & 0x80) != 0);
		regs.setZF(val == 0);
		regs.setHF(false);
		regs.setNF(false);
		regs.setPF(parityTable[val & 0xFF]);
		regs.setF3((val & 0x008) != 0);
		regs.setF5((val & 0x020) != 0);
	}


	public void SRL8(Register reg) {
		byte val = (byte)reg.getValue();
		regs.setCF((val & 0x01) != 0);
		val = (byte) ((val >> 1) & 0x7F);
		reg.setValue(val);
		regs.setSF((val & 0x80) != 0);
		regs.setZF(val == 0);
		regs.setHF(false);
		regs.setNF(false);
		regs.setPF(parityTable[val & 0xFF]);
		regs.setF3((val & 0x008) != 0);
		regs.setF5((val & 0x020) != 0);
	}

	public void SLI8(Register reg) {
		byte val = (byte)reg.getValue();
		regs.setCF((val & 0x80) != 0);
		val = (byte) ((val << 1) | 0x01);
		reg.setValue(val);
		regs.setSF((val & 0x80) != 0);
		regs.setZF(val == 0);
		regs.setHF(false);
		regs.setNF(false);
		regs.setPF(parityTable[val & 0xFF]);
		regs.setF3((val & 0x008) != 0);
		regs.setF5((val & 0x020) != 0);
	}


	
}
