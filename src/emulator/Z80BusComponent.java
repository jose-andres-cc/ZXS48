import java.util.HashMap;

// Clase base abstracta para todos los componentes del Bus de Z80 (Bus de Datos/ Bus de IO/Componentes conectados a los buses)
// Experimental
abstract class Z80BusComponentBase {
    // Método abstractos que deben ser implementado por las subclases

    // Gestión del direccionamiento de los componentes del bus
    public abstract int getMemStartAddress();

    public abstract int getMemRegionSize();

    public abstract int getIOStartAddress();

    public abstract int getIORegionSize();

    public abstract HashMap<Integer, Integer> getIORegionMap(); // Dispositivos de IO que tiene que utilizar un mapa de
                                                                // regiones y no una región contigua

    // Operaciones de memoria sobre el bus (escritura/lectura/lectura opcode M1 activo)
    public abstract void memWrite(int address, byte value);

    public abstract byte memRead(int address);

    public abstract byte memReadOpCode(int address);

    // Operaciones de IO sobre el bus (escritura/lectura)
    public abstract void ioWrite(int address, byte value);

    public abstract byte ioRead(int address);

}

// Clase genérica con implementación básica de los componentes del Bus de Z80
class Z80BusComponent extends Z80BusComponentBase {
    // Necestiamos reserva de memoria/IO porque habrá componentes que responderán a
    // todo y deberán poder gestionar todo
    // Dispositivos de memoria
    private final int memStartAddress;
    private final int memRegionSize;
    // Dispositivos de IO
    private final int ioStartAddress;
    private final int ioRegionSize;
    private final HashMap<Integer, Integer> ioRegionMap; // Mapa de regiones direccionables
    // Reservas de datos para memoria y datos de componentes tontos (sin lógica adicional)
    // Los componentes listos deberían poder indicar que no necesitan esta reserva
    // Y los de io finalmente no se si la necesitan o no, porque se supone que siempre son 'listos'
    private final byte[] memData; // Espacio para datos de memoria
    private final byte[] ioData; // Espacio para datos de entrada/salida

    // Multiples constructores

    // Constructor con indicación de tipo de componente
    public Z80BusComponent(int componentType, int startAddress, int regionSize) {
        int memStartAddress = 0;
        int memRegionSize = 0;
        int ioStartAddress = 0;
        int ioRegionSize = 0;
        if (componentType == Constants.MEM_COMPONENT) {
            // Dispositivos de memoria
            memStartAddress = startAddress;
            memRegionSize = regionSize;
        }
        if (componentType == Constants.IO_COMPONENT) {
            // Dispositivos de IO
            ioStartAddress = startAddress;
            ioRegionSize = regionSize;
        }
        if (componentType == Constants.Z80_BUS) {
            // Bus de datos (memoria/IO)
            memStartAddress = 0;
            memRegionSize = 0x10000;
            ioStartAddress = 0;
            ioRegionSize = 0x10000;
        }

        // Inicializaciones comunes a ambos tipos de dispositivos
        this.memStartAddress = memStartAddress;
        this.memRegionSize = memRegionSize;
        this.ioStartAddress = ioStartAddress;
        this.ioRegionSize = ioRegionSize;
        this.ioRegionMap = new HashMap<Integer, Integer>();
        // Reserva de memoria, solo para dispositivos
        if (componentType != Constants.Z80_BUS) {
            this.memData = new byte[memRegionSize];
            this.ioData = new byte[ioRegionSize];
        } else {
            this.memData = new byte[0];
            this.ioData = new byte[0];
        }
    }

    // Constructor para componentes mixtos de memoria e io
    public Z80BusComponent(int memStartAddress, int memRegionSize, int ioStartAddress, int ioRegionSize) {
        // Dispositivos de memoria
        this.memStartAddress = memStartAddress;
        this.memRegionSize = memRegionSize;
        // Dispositivos de IO
        this.ioStartAddress = ioStartAddress;
        this.ioRegionSize = ioRegionSize;
        this.ioRegionMap = new HashMap<Integer, Integer>();
        this.memData = new byte[memRegionSize];
        this.ioData = new byte[ioRegionSize];
    }

    // Para el caso de los dispositivos con la dirección autoincrustada necesitamos
    // un método set para inicializar el mapa de regiones asociadas
    // Antes de utilizar este método se inicializa la clase con (ioStartAddress=-1,
    // ioRegionSize=0)
    protected void setIORegionMap(HashMap<Integer, Integer> regionMap) {
        regionMap.forEach((start, size) -> {
            this.ioRegionMap.put(start, size);
        });
    }

    // Getters publicos (¿protected?)
    public int getMemStartAddress() {
        return memStartAddress;
    }

    public int getMemRegionSize() {
        return memRegionSize;
    }

    public int getIOStartAddress() {
        return ioStartAddress;
    }

    public int getIORegionSize() {
        return ioRegionSize;
    }

    public HashMap<Integer, Integer> getIORegionMap() {
        return ioRegionMap;
    }

    // Implementaciones muy básicas de lectura y escritura de memoria para un
    // componente

    public void memWrite(int address, byte value) {
        memData[(address & 0xFFFF) - (getMemStartAddress() & 0xFFFF)] = value;
    }

    public byte memRead(int address) {
        return memData[(address & 0xFFFF) - (getMemStartAddress() & 0xFFFF)];
    }

    public byte memReadOpCode(int address) {
        return memData[(address & 0xFFFF) - (getMemStartAddress() & 0xFFFF)];
    }

    ////////
    // Implementaciones muy básicas de lectura y escritura de entrada/salida para un componente

    public void ioWrite(int address, byte value) {
        ioData[(address & 0xFFFF) - (getIOStartAddress() & 0xFFFF)] = value;
    }

    public byte ioRead(int address) {
        return ioData[(address & 0xFFFF) - (getIOStartAddress() & 0xFFFF)];
    }

}
