import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// Clase Z80Bus que hereda de Z80BusComponent
// Soporte para todos los dispositivos que se conecten al bus, sean de memoria o de IO
// En estos momentos se puede calificar como experimental
class Z80Bus extends Z80BusComponent {
    private List<Z80BusComponentBase> components;
    private Z80BusComponentBase[] memPagedComponents;
    private Z80BusComponentBase[][] ioPagedComponents; // bus de IO del Z80 (8 bits declarado, 16 bits de
                                                       // direccionamiento no contiguo, soporte para más de un
                                                       // dispositivo por dirección)

    // Constructor para inicializar la lista de componentes
    public Z80Bus() {
        // Llamada al padre para fijar el máximo direccionamiento que va a tener el bus
        // (habitualmente 64K)
        // Es una llamada que NO reserva espacios de almacenamiento
        // Pero de momento no lo aplicamos porque creo que no lo necesitamos
        super(Constants.Z80_BUS, 0,0); //El bus siempre se inicializa a 64 kbytes sin reserva de espacio
        // Lista de todos los componentes
        components = new ArrayList<>();
        // Páginas de memoria, 64 x 1KB
        memPagedComponents = new Z80BusComponentBase[64]; // 64 espacios contiguos de 1 KB, sin gestión de _ROMCS
        Arrays.fill(memPagedComponents, null); // Inicializa todos los elementos a null (equivalente a ZeroMemory)
        ioPagedComponents = new Z80BusComponentBase[256][]; // 256 espacios contiguos de 256 direcciones, aunque podrían
                                                            // ser 65536, soporte para más de un dispositivo por
                                                            // dirección
        Arrays.fill(ioPagedComponents, null); // Inicializa todos los elementos a null (equivalente a ZeroMemory)

    }

    // Método para agregar un nuevo componente al bus
    public int addBusComponent(Z80BusComponentBase newComponent) {
        // Verifica si el componente ya existe en la lista
        for (Z80BusComponentBase component : components) {
            if (component == newComponent) {
                return Constants.RESULT_ERROR_ALREADY_EXISTS;
            }
        }
        components.add(newComponent);
        onComponentsUpdated();
        return Constants.RESULT_OK;
    }

    /////////////// Operaciones de lectura y escritura para este bus
    /// Memoria
    /// // Método para escribir un valor en una dirección específica
    public void memWrite(int address, byte value) {
        Z80BusComponentBase component = memPagedComponents[(address & 0xFFFF) / 1024];
        if (component != null) {
            component.memWrite(address, value);
        }
    }

    // Método para leer un valor desde una dirección específica
    public byte memRead(int address) {
        Z80BusComponentBase component = memPagedComponents[(address & 0xFFFF) / 1024];
        return (component != null) ? component.memRead(address) : (byte) 0xFF;
    }

    // Método para leer un valor desde una dirección específica una operación
    public byte memReadOpCode(int address) {
        Z80BusComponentBase component = memPagedComponents[(address & 0xFFFF) / 1024];
        return (component != null) ? component.memRead(address) : (byte) 0xFF;
    }

    ////////// IO
    ///
    // Método para escribir un valor en una dirección específica
    public void ioWrite(int address, byte value) {
        // BusComponentBase component = pagedComponents[(address & 0x00FF)]; //
        // Utilizamos el byte menos significativo para seleccionar dispositivo
        // if (component != null) {
        // component.write(address, value);
        // }
        if (ioPagedComponents[(address & 0x00FF)] != null) {
            for (Z80BusComponentBase component : ioPagedComponents[(address & 0x00FF)]) {
//                if (component != null) {
//                    System.out.printf( "Z80Bus.ioWrite address:%04x address&0x00FF:%04x byte:%02x \n", address, address & 0x00FF, value);
//                    System.out.printf("component #%02x: %s ", address & 0x00FF, component.getClass().getName());
                    component.ioWrite(address, value);
//                }
            }
            ;
        }
        ;
    }

    // Método para leer un valor desde una dirección específica
    public byte ioRead(int address) {
        // if ((address & 0x00FF) != 0xFE) System.out.printf( "BusIoZ80.read
        // address:%04x address&0x00FF:%04x \n", address, address & 0x00FF);
        // BusComponentBase component = pagedComponents[(address & 0x00FF) ]; //
        // Utilizamos el byte menos significativo para seleccionar dispositivo
        // return (component != null) ? component.read(address) : (byte) 0xFF;
        byte returnValue = (byte) 0xFF;
        if (ioPagedComponents[(address & 0x00FF)] != null) {
            for (Z80BusComponentBase component : ioPagedComponents[(address & 0x00FF)]) {
                byte value = component.ioRead(address);
                returnValue &= value;
            }
            ;
        }
        ;
        // if (returnValue != (byte)0xff) System.out.printf( "BusIOZ80 read:%02x \n",
        // returnValue);
        return returnValue;
    }

    // Método para eliminar un componente del bus
    public int removeBusComponent(Z80BusComponentBase component) {
        // Busca y elimina el componente si existe
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i) == component) {
                components.remove(i);
                onComponentsUpdated();
                return Constants.RESULT_OK;
            }
        }
        return Constants.RESULT_ERROR_NOT_FOUND;
    }

    // Método que se llama cuando los componentes se actualizan
    // Implementación específica puede ser sobreescrita en subclases
    protected void onComponentsUpdated() {

        // Construir la tabla de búsqueda rápida para cada componente.
        // El espacio de direcciones se divide en segmentos de 1 Kilobyte (64 entradas).
        Arrays.fill(memPagedComponents, null); // Resetea la tabla de paginación
        Arrays.fill(ioPagedComponents, null); // Resetea la tabla de paginación

        // JAC
        List<Z80BusComponentBase> components = getComponents();

        for (Z80BusComponentBase component : components) {
            if (component != null) {
                // Lo primero es saber que tipo de componente vamos a conectar, que puede ser
                // AMBOS
                // Añadir un componente aquí implica averiguar sus direcciones de memoria e IO
                // para ver si opera o no en el espacio de memoria y/o io
                // - Asignación del dispositivo de memoria al bus de datos
                if (component.getMemStartAddress() != 0 || component.getMemRegionSize() != 0) {
                    // Nota: no se contempla el mecanismo de _ROMCS
                    int start = component.getMemStartAddress() / 1024;
                    int end = start + (component.getMemRegionSize() / 1024);
                    for (int i = start; i < end; i++) {
                        memPagedComponents[i] = component;
                    }
                }
                // - Adaptador de IO
                if (component.getIOStartAddress() != 0 || component.getIORegionSize() != 0) {
                    if (component.getIOStartAddress() != -1) {
                        // Se trata de un componente sin mapa de regiones
                        ioPagedComponentsUpdate(component.getIOStartAddress(), component.getIORegionSize(), component);
                    } else {
                        // Aquí tenemos el mapa de regiones
                        HashMap<Integer, Integer> regionMap = component.getIORegionMap();
                        regionMap.forEach((start, size) -> {
                            ioPagedComponentsUpdate(start.intValue(), size.intValue(), component);
                        });
                    }
                }
            }
        }
    }

    private void ioPagedComponentsUpdate(int start, int size, Z80BusComponentBase component) {
        int address8Bits = start & 0x00FF; // dirección de 8 bits
        int endOf8BitsAddresses = address8Bits + size; // Esto hay que darle una vuelta --> 
                                                                // Le vamos a dejar reservar más de 1 espacio de 256
                                                               // direcciones si quiere
        Z80BusComponentBase[] arrayComponents;

        for (int i = address8Bits; i < endOf8BitsAddresses; i++) {
            System.out.printf("Z80Bus ioPagedComponentsUpdate address8Bits:%04x i:%04x \n", address8Bits, i);
            // Rellenamos el array de arrays
            // if (pagedComponents[i].length == 0){
            if (ioPagedComponents[i] == null) {
                arrayComponents = new Z80BusComponentBase[1];
                arrayComponents[0] = component;
            } else {
                // Ya hay algún dispositivo en ese espacio
                arrayComponents = new Z80BusComponentBase[ioPagedComponents[i].length + 1];
                int index = 0;
                for (Z80BusComponentBase componentCopy : ioPagedComponents[i]) {
                    arrayComponents[index] = componentCopy;
                    index++;
                }
                arrayComponents[index] = component;
            }
            // Se lo asignamos al espacio de direcciones
            ioPagedComponents[i] = arrayComponents;
        }

    }

    // Metodo para acceder a la lista de componentes
    protected List<Z80BusComponentBase> getComponents() {
        return components;
    }

   	public void outputComponentsList(){

		int j = 0;
		for (int i =0; i<256;i++)
		{
			//System.out.printf("Address: %02x ", i);
				if (ioPagedComponents[(i & 0x00FF) ] != null){
					for (Z80BusComponentBase component : ioPagedComponents[(i & 0x00FF)]) {
						if (j==0) System.out.printf("Address: %02x ", i);
						System.out.printf("component #%02x: %s ", j, component.getClass().getName());
						j++;
					}
					j=0;
					System.out.printf("\n");
				}
				//else System.out.printf(" empty \n");
		}
	}

}
