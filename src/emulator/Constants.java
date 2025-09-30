//Clase contenedora de datos constantes del emulador

public class Constants {

	// Constantes para identificar internamente interfaces (interfaceId)
	public static final int ZXINTERFACE1 	= 1;
	public static final int ZXINTERFACE2 	= 2;
	public static final int KEMPSTON	 	= 3;
	public static final int CURSORESINTERFACE	 	= 4;
	

	//Constantes para el dispositivo joystick
	public static final int JOYSTICK_RIGHT 	= 1;
	public static final int JOYSTICK_LEFT 	= 2;
	public static final int JOYSTICK_DOWN	= 3;
	public static final int JOYSTICK_UP		= 4;
	public static final int JOYSTICK_FIRE	= 5;

	// Constantes para la clase Bus y herederas
	// - Resultados de la llamada para añadir componente
    public static final int RESULT_OK = 0;
    public static final int RESULT_ERROR_ALREADY_EXISTS = 1;
    public static final int RESULT_ERROR_NOT_FOUND = 2;
	// - Tipos de componente del bus
	public static final int MEM_COMPONENT = 1;
	public static final int IO_COMPONENT = 2;
	public static final int Z80_BUS = 3;

	// Constantes para la velocidad del reloj
    public static final int CLOCK_SPEED_NORMAL = 0;
    public static final int CLOCK_SPEED_UNLIMITED = 99;

	 
}