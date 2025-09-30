// Clase CharDevice que hereda de Z80BusComponent
// Dispositivo a conectar al bus IO para sacar un texto por la salida de sistema 
// Conexión: El dispositivo se paginará sobre 2 direcciones del IOBus
// En la Dirección startAddress+0 se activa el modo de funcionamiento del dispositivo
// Modo 0: Dispositivo inactivo
// Modo 1: El dato escrito en la dirección startAddress+1 se imprime en la salida
// Modo 2: El dato escrito en la dirección startAddress+1 se acumula en un buffer interno
// Modo 3: Funciona como un disparador, si estamos en modo 2 imprime el buffer, vacia buffer y vuelve a modo 0
// Nota: controlamos lo minimo

class CharDevice extends Z80BusComponent {
    private StringBuilder buffer = new StringBuilder();
	private byte modo;

    // Constructor para inicializar el dispositivo
	// El dispositivo se mapea en las direcciones que se le asigne en la creación pero luego solo utilizará
	// las dos primeras direcciones y considerando solo la parte baja del bus de direcciones
    public CharDevice(int startAddress, int regionSize) {
        super(Constants.IO_COMPONENT, startAddress, regionSize);
    }


    // Método para escribir un valor en una dirección específica y que recibe el dispositivo (Z80 escribe, dispositivo lee)
	// Solo se considera la parte baja del bus de direcciones
    public void ioWrite(int address, byte value) {
		if ((address & 0x00FF) == getIOStartAddress()){
			//Es un comando
			// value==0 -> activar Modo 0: Dispositivo inactivo -> no hacer nada
			// value==1 -> activar Modo 1: El dato escrito en la dirección 101 se imprime en la salida
			switch (value){
				case 1:
					modo = 1;
					break;
			// value==2 -> activar Modo 2: El dato escrito en la dirección 101 se acumula en un buffer interno
				case 2:
					modo = 2;
					break;
			// value==3 -> activar Modo 3: Funciona como un disparador, si estamos en modo 2 imprime el buffer, vacia buffer y vuelve a modo 0
				case 3:
					printBuffer();
					modo = 0;
					break;
			}
		}
		if ((address & 0x00FF) == getIOStartAddress() + 1){
			if (modo == 2){
				// Almacenamos los caracteres recibidos en el buffer
				buffer.append((char) value);
			}
			else if (modo == 1){
				// Imprimimos el caracter recibido
				System.out.printf("%c", (char)value);
			}
		}

    }


    // Método que imprime los caracteres almacenados en el buffer
    private void printBuffer() {
        System.out.printf("%s", buffer.toString());
        buffer.setLength(0); // Limpia el buffer después de imprimir
    }

    // Método para leer un valor desde una dirección específica
	//Devolvemos siempre 0
    public byte ioRead(int address) {
        return 0;
    }
}
