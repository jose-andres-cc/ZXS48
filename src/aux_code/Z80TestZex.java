/***********************************************************************************************
* Launcher for the Frank Cringle's Z80 Instruction Set Exerciser
* 
* Copyright (c) 2019 Nicolas Allemand
* Copyright (c) 2025 Jose Andres Calvo Conde
* 
* This is a port from C99 to Java for code https://github.com/superzazu/z80/blob/master/z80_tests.c
* 
* See Z80TestZex.md for additional info.
************************************************************************************************/

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class Z80TestZex {
    private static final int MEMORY_SIZE = 0x10000; //64 Kb RAM si no los tests fallan
    private static final int BUFFER_SIZE = 0x04000; //16 Kb para los ficheros de ROM
    private static byte[] memoryBuffer = new byte[BUFFER_SIZE]; // Buffer para leer las ROMS de fichero
    private static boolean testFinished = false;

    // Cargar archivo de ROM en la memoria
    private static int loadFile(String filename, Z80Bus db, int addr) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            System.err.println("error: can't open file '" + filename + "'.");
            return 1;
        }

        long fileSize = file.length();
        if (fileSize + addr >= MEMORY_SIZE) {
            System.err.println("error: file " + filename + " can't fit in memory.");
            return 1;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = fis.read(memoryBuffer, addr, (int) fileSize);
            if (bytesRead != fileSize) {
                System.err.println("error: while reading file '" + filename + "'");
                return 1;
            }
        }
		for (int i = addr; i< (addr + fileSize); i++){
			db.memWrite((short)i, memoryBuffer[i]);
		}
        return 0;
    }

    // Ejecutar el test
    private static int runTest(Z80 z, String filename, long tStatesExpected) throws IOException {
		
		//Accedemos al bus de datos para manipular la memoria
		Z80Bus zDB = z.getDataBus();

        if (loadFile(filename, zDB, 0x100) != 0) {
            return 1;
        }


        System.out.println("*** TEST: " + filename);
        //z.pc = 0x100;
		Z80Registers zRegs = z.getRegisters();
		zRegs.setPC((short) 0x100);

		// Direccion 0x00: Salir del test
        // Inyectamos en la dirección 0 el código para detener el test
		// Para detener el test ponemos a 0xFF la dirección de memoria 0x16
        // El programa verifica el dato de la dirección 0x16 cada ciclo de ejecucion

//   Ensamblador
//                          .ORG   0x0   
//   18 0E                  JR   fin_test   
//                          .ORG 0x10
//   3E FF        FIN_TEST: LD   a,0xff   
//   32 16 00               LD   (MARCA_FIN_TEST),a   
//   76                     HALT   
//   00           MARCA_FIN_TEST:   NOP   

		zDB.memWrite(0x0000, (byte) 0x18); //JR   fin_test
		zDB.memWrite(0x0001, (byte) 0x0E);
		zDB.memWrite(0x0010, (byte) 0x3E); //LD   a,0xff
		zDB.memWrite(0x0011, (byte) 0xFF);
		zDB.memWrite(0x0012, (byte) 0x32); //LD   (marca_fin_test),a
		zDB.memWrite(0x0013, (byte) 0x16);
		zDB.memWrite(0x0014, (byte) 0x00);
		zDB.memWrite(0x0015, (byte) 0x76); //Halt
		zDB.memWrite(0x0016, (byte) 0x00); //MARCA_FIN_TEST:   NOP 
		
		// Direccion 0x05: Impresion de caracteres
		// Inyectamos en la direccion 5 la impresion de caracteres

//  Código en Java a codificar en ensamblador
//		  byte operation = z.c;
//        if (operation == 2) {
//            System.out.print((char) z.e);
//        } else if (operation == 9) {
//            int addr = (z.d << 8) | (z.e & 0xFF);
//            do {
//                System.out.print((char) rb(addr++));
//            } while (rb(addr) != '$');
//        }
//        return (byte) 0xFF;

//   Ensamblador
//                       .ORG   0x05   
//18 49                  JR   IMPRIMIR   
//
//                       .ORG   0x50
//                       ;operacion = registro_C -> valores 2 y 9
//79           IMPRIMIR:   LD   a,c   
//FE 02                  CP   2   
//20 08                  JR   nz,sigue   
//                       ;operacion == 2
//3E 01                  LD   a,1   
//D3 64                  OUT   (100),a   
//7B                     LD   a,e   
//D3 65                  OUT   (101),a   
//C9                     RET   
//FE 09        SIGUE:    CP   9   
//                       ; si no es 9 nos vamos
//C0                     RET   nz   
//                       ;operacion == 9
//3E 02                  LD   a,2   
//D3 64                  OUT   (100),a   
//1A           SIGUIENTE:   LD   a,(de)   
//FE 24                  CP   '$'   
//28 05                  JR   Z,sacar_texto   
//D3 65                  OUT   (101),a   
//13                     INC   de   
//18 F6                  JR   siguiente   
//3E 03        SACAR_TEXTO:   LD   a,3   
//D3 64                  OUT   (100),a   
//C9                     RET   

        //Salto desde 0x05 a IMPRIMIR
		zDB.memWrite(0x05, (byte)0x18);  //JR   IMPRIMIR
		zDB.memWrite(0x06, (byte)0x49);  


		String hexString = "79FE0220083E01D3647BD365C9FE09C03E02D3641AFE242805D3651318F63E03D364C9";
		short ptr = (short)0x0050; //dirección de la rutina en memoria

        // Recorrer la cadena de dos en dos caracteres y volcarla en memoria
        for (int i = 0; i < hexString.length(); i += 2) {
            // Obtener el par de caracteres
            String hexPair = hexString.substring(i, i + 2);
            // Convertir el par hexadecimal a un número decimal
            int number = Integer.parseInt(hexPair, 16);
			zDB.memWrite(ptr, (byte)number);ptr = (short)(ptr +1);  
		}


        // Iniciamos el test
        testFinished = false;

        while (!testFinished) {
			z.execInst();
			z.setTStates(0);
			if ((zDB.memRead(0x0016) & 0xFF) == 0xFF) {
				testFinished = true;
			} 
        }

        System.out.println("");
        System.out.println("*** FINISHED TEST: " + filename);

        return 0; // Sin control de ejecucion
    }

    public static void main(String[] args) {
        // procesador
        Z80 cpu = new Z80();         
        // 1KB * 64 de memoria RAM, si no los test fallan (no inicializan SP de acuerdo a la memoria)
        Z80BusComponent ram = new Z80BusComponent(Constants.MEM_COMPONENT,0, 1024 * 64);
        // Bus de datos
        Z80Bus dataBus = new Z80Bus();  
        // Conectar RAM al bus
        dataBus.addBusComponent(ram);
        // Dispositivo de salida por pantalla de tipo caracter
		CharDevice charDevice = new CharDevice(100,2); // Dirección 100 y 101
        dataBus.addBusComponent(charDevice);
        // Conectar bus a la CPU
        cpu.setDataBus(dataBus);

        System.out.println("Z80TestZex - Frank Cringle's Z80 Instruction Set Exerciser");
        System.out.println("Z80 processor + 64 KB RAM + CharDevice");

        // Verificación de componentes conectados al bus
        dataBus.outputComponentsList();

        System.out.println("*** Launching tests...");

        try {
            int result = 0;
            result += runTest(cpu, "roms/test/FDC_tests/prelim.com", 10140L); // Los datos de tstatesExpected no se usan.
            result += runTest(cpu, "roms/test/FDC_tests/zexdoc.cim", 46734978649L); 
            result += runTest(cpu, "roms/test/FDC_tests/zexall.cim", 46734978649L);

            System.exit(result != 0 ? 1 : 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
