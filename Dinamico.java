/*
 * Copyright 2021-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.foo.app;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.core.Version;
import org.onosproject.core.CoreService;
import org.onosproject.net.host.HostService;
import org.apache.karaf.shell.api.action.Argument;
import java.util.*;
import org.onosproject.net.PortNumber;
import org.onosproject.net.DeviceId;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.Link;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.DeviceId.deviceId;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import java.io.*;
import static org.onosproject.cli.net.DevicesListCommand.getSortedDevices;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.device.PortStatistics;
import com.google.common.collect.Lists;
import static com.google.common.collect.Lists.newArrayList;
import org.onosproject.net.ConnectPoint;
import java.util.concurrent.*;
import org.onosproject.net.intent.IntentService;
/*
* 
*
*Sample Apache Karaf CLI command XD.
*/
//anotacion que agrega un servicio al controlador onos a traves de la funcion AppCommand
@Service
//anotacion que agrega un comando al CLI
@Command(scope = "onos", name = "sample",
         description = "Sample Apache Karaf CLI command")
public class AppCommand extends AbstractShellCommand {
    //anotacion que permite exigir un parametro de entrada llamado Nserv
    @Argument(index = 0, name = "Nserv", description = "Numero de servidores",required = true, multiValued = false) // al ejecutar el comando se requieren ciertos argumentos
    // variable que simboliza el numero de servidores inicializada en 0, al momento de pedir el valor por medio del CLI cambia el valor de la misma por el valor ingresado
    int Nserv = 0;
    
    // bandera de inicializacion variables algoritmo de volacado de trafico
    public static boolean flag = true;
    public static int guardaSecuencia = 1200;
    public static ArrayList<Boolean> flag_time = new ArrayList<Boolean>(); //bandera de tiempo.
    public static ArrayList<Integer> t_c = new ArrayList<Integer>();// Tiempo de espera para volver a tomar una decisión sobre un host especifico.
    public static ArrayList<Float> time_past = new ArrayList<Float>(); //tiempo pasado
    public static ArrayList<Float> pktRx_past = new ArrayList<Float>(); //recepcion pasada
    public static ArrayList<Float> pktTx_past = new ArrayList<Float>(); // transmision psada
    public static ArrayList<Float> bytesRx_past = new ArrayList<Float>(); //bytes pasados en Rx
    public static ArrayList<Float> bytesTx_past = new ArrayList<Float>(); // bytes pasdos en Tx
    
    //Arreglos para los datos a promediar
    public static ArrayList<Float> rateRx_av = new ArrayList<Float>(); //rango Rx promedio
    public static ArrayList<Float> rateTx_av = new ArrayList<Float>();// rango Tx promedio
    public static ArrayList<Float> delayRx_av = new ArrayList<Float>();// retraro Rx promedio
    public static ArrayList<Float> delayTx_av = new ArrayList<Float>();// retraso Tx promedio

    
    private static int kp = 10; // tp = ts*kp; kp: Numero de datos a promediar
    private static long t_s = 100; // Tiempo de sondeo [ms]
    private static int  kc = 4; // tc = tp*kc = ts*kp*kc
    
    private static float BW1 = 2000000; // Ancho de banda del canal con menor ancho de banda
    private static float Lim_rate1 = 2000000; //Limite de tasa [bits/s]
    private static float Lim_delayTx1 = 0.01f; // Limite del delay de Tx[s]
    private static float Lim_delayRx1 = 0.01f; // Limite del delay de Rx [s]
    
    private static float BW2 = 4000000; // Ancho de banda del canal con menor ancho de banda
    private static float Lim_rate2 = 4000000; //Limite de tasa [bits/s]
    private static float Lim_delayTx2 = 0.02f; // Limite del delay de Tx[s]
    private static float Lim_delayRx2 = 0.02f; // Limite del delay de Rx [s]
    
    private static float BW3 = 6000000; // Ancho de banda del canal con menor ancho de banda
    private static float Lim_rate3 = 6000000; //Limite de tasa [bits/s]
    private static float Lim_delayTx3 = 0.03f; // Limite del delay de Tx[s]
    private static float Lim_delayRx3 = 0.03f; // Limite del delay de Rx [s]
    
    private static final String FORMAT = "port=%s \n rateRx=%s, rateTx=%s, \ndelayTx=%s, Dur=%s\n"; //crea variable de  formato
    public static int cambio = 0; // variblable que indica si hubo cambio o no.
    
    /* Devuelve el puerto asociado (Id dispositivo, numeropuerto)*/
    private String portAssociate(DeviceId devId, PortNumber portN){
	//obtiene el servicio de enlace
	LinkService service = get(LinkService.class);
	//variable que encuentra el destino
	String dest = "";
	//ciclo que obitnene todos los links del diposicitvo asociado
	for(Link link : service.getDeviceLinks(devId)){
	    //aux1 obtiene dispositivo y puerto de todos los links
	    String aux1 = link.src().deviceId()+"/"+link.src().port();
	    //aux2 guarda dispositivo y puerto de entradas
	    String aux2  = devId+"/"+portN; 
	    
	    //si algun link es igual a el de entrada encuentra el destino
	    //de lo contrario el destino es vacio.
	    if(aux1.equals(aux2)){
	        dest = link.dst().deviceId()+"/"+link.dst().port();
	    }
	}
    return dest;
    }
    
    //Devuelve el nuemro de enlaces que ingresan al dispositivo
    private int NumLinksDevice(DeviceId devId){
    	//obtiene el servicio de enlace
    	LinkService service = get(LinkService.class);
    	//crea un contador que almacenara el numero de enlaces
    	int NumLinks = 0;
   	for (Link link : service.getDeviceIngressLinks(devId)){
    	     NumLinks++;
    	}
	return NumLinks;
    }
    
    // funcion que devuelve el ID de un dispositivo
    private String getDeviceId(String deviceString){
	//variable enterea que guarda la poscion del Slash en el arreglo    
	int slash = deviceString.indexOf("/");
	//si la poscicion es cero no deuvleve nada
	if(slash <= 0){
	    return "";
	}	
	//toma los caracteres desde 0 hasta el slash 			
	return deviceString.substring(0,slash);
    }
    // funcion que duelve el numero del puerto de un dipositivo
    private String getPortNumber(String deviceString){
	// variable entrega la posicion en la que se ubica el slash dentro del arreglo
        int slash = deviceString.indexOf("/");
        if(slash <= 0){
	     return "";
	}
	// devuelve un string con los caracteres desde slash+1 hasta la ultima dimension del string
	return deviceString.substring(slash + 1, deviceString.length());
    }

    //Estado del puerto - set
    private void portState(String deviceID, String Nport, boolean state){
        //Clase que proporciona funcionalidad para la gestión de dispositivos
	DeviceService deviceService = get(DeviceService.class);
	// Proporciona administracion de los serivicios.
	DeviceAdminService deviceAdminService = get(DeviceAdminService.class);
	//Devuelve el dispositivo con el identificador especificado.
	Device dev = deviceService.getDevice(DeviceId.deviceId(deviceID));
	//obtiene el numero del puerto de un string
	//print(""+dev);
	PortNumber pnum = PortNumber.fromString(Nport);
	//Devuelve el puerto con el punto de conexión especificado.
	//print(""+pnum);
	Port p = deviceService.getPort(dev.id(),pnum);
	//print(""+p);
	//Habilita o deshabilita administrativamente un puerto en un dispositivo
	//deviceId - identificador de dispositivo
	//portNumber - identificador de puerto
	//enable - verdadero si el puerto se va a habilitar, falso para deshabilitar
	deviceAdminService.changePortState(dev.id(),p.number(), state);
    }
    
    //Funcion que sirve para cargar un archivo plano con los datos del algoritmo (datos,bandera cambio.).
    private void WriteFile(String data, boolean flag){
    	//guarda la ruta del archivo plano.
	FileWriter fichero = null;
	// objeto al cual se le cargara la informacion.
	PrintWriter pw = null;
	//cargar un arhivo plano requiere de un try catch.
	try{
	    //Se escribe la ruta donde se quiere cargar el archivo plano con la bandera de cambio.
	    fichero = new FileWriter("/home/alcantaragomez/Data.txt", flag);
	    pw = new PrintWriter(fichero);
	    pw.println(data);
	}catch(Exception e){}
	finally{
	    try{
	        if(null != fichero)
		fichero.close();
	    }catch(Exception e2){}
	}
    }
    //Devuelve una lista ordenada de las estadisticas del puerto segun el numero de puertos.
    private static List<PortStatistics> sortByPort(Iterable<PortStatistics> portStats)  {
	//Convierte un iterable PortStatistics  en una lista PortStatistics.
        List<PortStatistics> portStatsList = Lists.newArrayList(portStats);
	//Ordena esta lista según el orden inducido por el especificado Comparator.
	portStatsList.sort(Comparator.comparing(ps -> ps.portNumber().toLong()));
	return portStatsList;
    }
    
    // Return port state
	private boolean NportState(String uri, int Nport){
	    DeviceService service = get(DeviceService.class);
	    Device device = service.getDevice(deviceId(uri));
	    if(device == null){
	    }else{
		boolean aux = false;
		for(Port port : service.getPorts(device.id())){
		    if(aux){
			if(!port.isEnabled() && Integer.parseInt(port.number().toString())==Nport ){
			    return false; //Nport disable
			}
		    }
		    aux = true;
		}
		return true; //Nport enable
	    }
	        return true; // No find device
	}

    //Algortimo de volcado de trafico
    private float printPortStats(DeviceId deviceId, Iterable<PortStatistics> portStats,int Ndevice, int sequence ){
	//Llama a la interface para poder obtener estadisticas del sistema
        StatisticService service = get(StatisticService.class);
	//inicio de interfaces de red
	
	/*
	Almacena en memoria la cantidad de interfaces activas por switch probadas
	*/
	int cont = 1;
	
	//Contador accion pasada
	int cont2 = 1+(Ndevice-1)*5; 
	
	//Contador parametros promedio
	int cont3 = (Ndevice-1)*5*kp;
	
	//bandera que indica el cambio de canal
	boolean flag_change = false;
	//bandera auxiliar.
	boolean flag_aux = true;
	
	float throughput= 0;
	//Iterable que contiene toda infromacion de los dispositivos de red.
	for(PortStatistics stat : sortByPort(portStats)){
	    if(cont <= 5){ //No. de interfaces de red
	    	//imprime estadisticas del puerto 1
	        print(""+stat);
		//Variable que guarda el dispositivo y el puerto en uso
	        ConnectPoint cp = new ConnectPoint(deviceId, stat.portNumber());
		//Guarda el tiempo de duracion de las estadisticas.
		float time = ((float)stat.durationSec()) +(((float)stat.durationNano())/TimeUnit.SECONDS.toNanos(1));	
		// Guarda el numero de paquetes recividos
		float pktRx = stat.packetsReceived();
		// Guarda el numero de paquetes transmitidos
		float pktTx = stat.packetsSent();
		// Guarda el numero de bytes recividos
		float bytesRx = stat.bytesReceived();
		// Guarda el numero de bytes enviados.
		float bytesTx = stat.bytesSent();
		
		//Calculate rate Rx/Tx [bit/s]
		float rateRx = ((bytesRx-bytesRx_past.get(cont2))*8)/(time-time_past.get(cont2));
		float rateTx = ((bytesTx-bytesTx_past.get(cont2))*8)/(time-time_past.get(cont2));
		
		//calculate Delay [s]
		float delayRx = (time-time_past.get(cont2))/((pktRx-pktRx_past.get(cont2))*8);
		float delayTx = (time-time_past.get(cont2))/((pktTx-pktTx_past.get(cont2))*8);
				time_past.set(cont2,time);
		
		//0   1     2      3        
		//ll cont  cont2  cont3
		throughput = rateRx + throughput;
		//Carga datos a los arreglos		
		pktRx_past.set(cont2,pktRx);
		pktTx_past.set(cont2,pktTx);
		bytesRx_past.set(cont2,bytesRx);
		bytesTx_past.set(cont2,bytesTx);
		
		//guarda la posicion de datos.
		int pos = cont3+((cont-1)*kp)+sequence%kp;
		if(cont >= 2){
		    pos = cont3+((cont-1)*kp)+sequence%kp+1;
		}
		
		if(pos == 0){pos = kp;}
		
		//Carga datos a los arreglos en la posicion dada.
		rateRx_av.set(pos,rateRx);
		rateTx_av.set(pos,rateTx);
		delayRx_av.set(pos,delayRx);
		delayTx_av.set(pos,delayTx);
		
		// Si se realizo cambio de puerto, espera x segundos para volver a tomar una decisión
		//sobre dicho host
		if( ( (sequence>(t_c.get(cont2))) || !flag_time.get(cont2) ) && (sequence%kp)==0 ){
		    if(flag_time.get(cont2)){
			flag_time.set(cont2,false);
		    } 
		    //Inicializa variables
		    rateRx = 0;
		    rateTx = 0;
		    delayRx = 0;
		    delayTx = 0;
		    //auxiliares para guardar la sumatoria de los valores y poder obtener el promedio
		    int aux_1 = 0;
		    int aux_2 = 0;
		    int aux_3 = 0;
		    int aux_4 = 0;
		    
		    if(cont >= 2){
		        for(int i = (cont3+((cont-1)*kp)+1); i<= (cont3+((cont-1)*kp)+kp);i++ ){
		        if(!Float.toString(rateRx_av.get(i)).equals("NaN")) { rateRx = rateRx+rateRx_av.get(i); }
			else { aux_1++; }
			if(!Float.toString(rateTx_av.get(i)).equals("NaN")) { rateTx = rateTx+rateTx_av.get(i); }
			else { aux_2++; }
			if(!Float.toString(delayRx_av.get(i)).equals("NaN")) { delayRx = delayRx+delayRx_av.get(i); }
			else { aux_3++; }
			if(!Float.toString(delayTx_av.get(i)).equals("NaN")) { delayTx = delayTx+delayTx_av.get(i); }
			else { aux_4++; }
		        }
		    }else{
		    	for(int i = (cont3+((cont-1)*kp)); i<= (cont3+((cont-1)*kp)+kp);i++ ){
		        if(!Float.toString(rateRx_av.get(i)).equals("NaN")) { rateRx = rateRx+rateRx_av.get(i); }
			else { aux_1++; }
			if(!Float.toString(rateTx_av.get(i)).equals("NaN")) { rateTx = rateTx+rateTx_av.get(i); }
			else { aux_2++; }
			if(!Float.toString(delayRx_av.get(i)).equals("NaN")) { delayRx = delayRx+delayRx_av.get(i); }
			else { aux_3++; }
			if(!Float.toString(delayTx_av.get(i)).equals("NaN")) { delayTx = delayTx+delayTx_av.get(i); }
			else { aux_4++; }
		        }
		    }
		    
		    
		    //Datos promediados
		    rateRx = rateRx/(kp-aux_1);
		    rateTx = rateTx/(kp-aux_2);
		    delayRx = delayRx/(kp-aux_3);
		    delayTx = delayTx/(kp-aux_4);
		    
		    if(flag_aux){
			print("\ndeviceId=%s", deviceId);
			flag_aux = false;
		    }
		    
		    if(Float.toString(delayTx).equals("Infinity")){ delayTx=0; }
		    if(Float.toString(delayRx).equals("Infinity")){ delayRx=0; }
		    print(FORMAT, stat.portNumber(), rateRx, rateTx, delayRx, delayTx, time);
		    if(sequence > guardaSecuencia + 20){ 
		    if(  ((rateTx+rateRx)>Lim_rate3) ||  ( (delayTx>Lim_delayTx3)&&((rateTx+rateRx)>(BW3/2)) ) ||  ( (delayRx>Lim_delayRx3)&&((rateTx+rateRx)>(BW3/2)) )  )  {    
		             String deviceAsso = portAssociate(deviceId("of:000000000000000"+Ndevice),
					      portNumber("5")); //device/port associate	      
			    if(!deviceAsso.equals("") && !flag_change && sequence!=1 ){
			        
			        String uriAsso = getDeviceId(deviceAsso);
			        String NportAsso = getPortNumber(deviceAsso);
			        IntentService intentService = get(IntentService.class);
			        boolean NportS  =NportState(uriAsso,Integer.parseInt(NportAsso));
			        if(NportS){
				    try{
				        portState("of:000000000000000"+3,"1",true);
				    	portState(getDeviceId(deviceAsso),getPortNumber(deviceAsso),false);
				        print("Cambio de puerto realizado en host" + Ndevice+"\n");
				        
				        // guarda tiempo en que se debe volver a realizar cambio de puerto
				        t_c.set(cont2-1,sequence+kp*kc);
				        //Bandera para informar que se realizo el cambio en determinado puerto
				        flag_time.set(cont2-1,true);
				        flag_change = true;
				    }catch(Exception e){}
			        }
			    } 	   
		   }else if( ((rateTx+rateRx)>Lim_rate2) ||  ( (delayTx>Lim_delayTx2)&&((rateTx+rateRx)>(BW2/2)) ) ||  ( (delayRx>Lim_delayRx2)&&((rateTx+rateRx)>(BW2/2)) )  )  {    
		             String deviceAsso = portAssociate(deviceId("of:000000000000000"+Ndevice),
					      portNumber("5")); //device/port associate	      
			    if(!deviceAsso.equals("") && !flag_change && sequence!=1 ){
			        
			        String uriAsso = getDeviceId(deviceAsso);
			        String NportAsso = getPortNumber(deviceAsso);
			        IntentService intentService = get(IntentService.class);
			        boolean NportS  =NportState(uriAsso,Integer.parseInt(NportAsso));
			        if(NportS){
				    try{
				        portState("of:0000000000000002","3",true);
				    	portState(getDeviceId(deviceAsso),getPortNumber(deviceAsso),false);
				        print("Cambio de puerto realizado en host" + Ndevice+"\n");
				        
				        // guarda tiempo en que se debe volver a realizar cambio de puerto
				        t_c.set(cont2-1,sequence+kp*kc);
				        //Bandera para informar que se realizo el cambio en determinado puerto
				        flag_time.set(cont2-1,true);
				        flag_change = true;
				    }catch(Exception e){}
			        }
			    } 	   
		   }else if( ((rateTx+rateRx)>Lim_rate1) ||  ( (delayTx>Lim_delayTx1)&&((rateTx+rateRx)>(BW1/2)) ) ||  ( (delayRx>Lim_delayRx1)&&((rateTx+rateRx)>(BW1/2)) )   )  {    
		             String deviceAsso = portAssociate(deviceId("of:000000000000000"+Ndevice),
					      portNumber("5")); //device/port associate	      
			    if(!deviceAsso.equals("") && !flag_change && sequence!=1 ){
			        
			        String uriAsso = getDeviceId(deviceAsso);
			        String NportAsso = getPortNumber(deviceAsso);
			        IntentService intentService = get(IntentService.class);
			        boolean NportS  =NportState(uriAsso,Integer.parseInt(NportAsso));
			        if(NportS){
				    try{
				        portState("of:0000000000000002","2",true);
				    	portState(getDeviceId(deviceAsso),getPortNumber(deviceAsso),false);
				        print("Cambio de puerto realizado en host" + Ndevice+"\n");
				        
				        // guarda tiempo en que se debe volver a realizar cambio de puerto
				        t_c.set(cont2-1,sequence+kp*kc);
				        //Bandera para informar que se realizo el cambio en determinado puerto
				        flag_time.set(cont2-1,true);
				        flag_change = true;
				    }catch(Exception e){}
			        }
			    } 	   
		   }else if( ((rateTx+rateRx)>0) ||  ( (delayTx>Lim_delayTx1)&&((rateTx+rateRx)>0) ) ||  ( (delayRx>Lim_delayRx1)&&((rateTx+rateRx)>0) )   )  {    
		             String deviceAsso = portAssociate(deviceId("of:000000000000000"+Ndevice),
					      portNumber("5")); //device/port associate	      
			    if(!deviceAsso.equals("") && !flag_change && sequence!=1 ){
			        
			        String uriAsso = getDeviceId(deviceAsso);
			        String NportAsso = getPortNumber(deviceAsso);
			        IntentService intentService = get(IntentService.class);
			        boolean NportS  =NportState(uriAsso,Integer.parseInt(NportAsso));
			        if(NportS){
				    try{
				        portState("of:0000000000000002","1",true);
				    	portState(getDeviceId(deviceAsso),getPortNumber(deviceAsso),false);
				        print("Cambio de puerto realizado en host" + Ndevice+"\n");
				        
				        // guarda tiempo en que se debe volver a realizar cambio de puerto
				        t_c.set(cont2-1,sequence+kp*kc);
				        //Bandera para informar que se realizo el cambio en determinado puerto
				        flag_time.set(cont2-1,true);
				        flag_change = true;
				    }catch(Exception e){}
			        }
			    } 	   
		   }
		   
		   }
		   
		   

			WriteFile(sequence+","+deviceId+","+stat.portNumber()+","+rateRx+","+rateTx+","+delayRx+","+delayTx+","+time+","+cambio,true);
		    
		}
	        cont2++;
	    }
	    cont++; 
	}
	return throughput;
	
    }
    
    

    @Override
    protected void doExecute() {
        //el metodo get proviene de AbstractShellCommand permite traer la clase
	//Clase que proporciona funcionalidad para la gestión de dispositivos.
	DeviceService deviceService = get(DeviceService.class);
	
	//Servicio de administración del inventario de dispositivos de infraestructura.
	DeviceAdminService deviceAdminService = get(DeviceAdminService.class);

	//obtiene el numero de hosts, para el caso de la arquitectura propuesta este valor es 3
	int numHosts = get(HostService.class).getHostCount(); 
	print("host=%d",numHosts);

	//Numero de servidores en la red
	print("servers=%s",Nserv);
	
	//se calcula el numero de host reales ya que alguna maquina puede funcionar de servidor, por esta razon se resta el numero de maquinas menos el numero de host ingresados por pantalla 
	int Nhost_Real = numHosts-Nserv;
	
	if(flag){
	    //crea una variable auxiliar para rellenar con ceros.
	    float aux = 0;
	    /*
	    Este ciclo for cumple la funcion de inicialziar los espacios en memoria
	    Que permitiran acumular las acciones pasadas del algortimo.
	    Para este caso solo se requiere acumular dos acciones pasadas(puerto1 y puerto2) por cada cliente y al final
	    se suma un valor de uno al limite del for para tener en cuenta el corrimiento de cada
	    arreglo ya que cada uno empieza en 0 
	    */
	    for(int i = 1;i <= (Nhost_Real*5+1);i++ ){
		//rellena flag_time 5 veces con false
	        flag_time.add(false);
	        //rellena todos los arreglos con 5 ceros	
		t_c.add(0); 
		time_past.add(aux);
		pktRx_past.add(aux);
		pktTx_past.add(aux);
		bytesRx_past.add(aux);
		bytesTx_past.add(aux);
	    }
	    /*
	    Este ciclo for elimina las conexiones bases y solo deja un camino entre host y servidor
	    El for requiere saber cuantos enlaces entran hacia el SW2 para saber hasta dodne llegara.
	    Las conexion 1 y 6 se ignoran porque corresponden al puerto del servidor
	    y al puerto de salida del SW2 que conecta con el SW4.
	    H1 -6(SW1)1 - 1(SW2)6 - H2 (puertoEntrante(Switch)puertoSaliente)  
	    Se le suma uno al limite del ciclo for ya que la funcion NumLink no devuelve las conexiones de host solo de switch
	    */
	    for(int i = 1; i <= NumLinksDevice(deviceId("of:0000000000000001"))+1; i++){
		//Devuelve cada SW asociado con el puerto al cual el SW1 esta haciendo uso en las conexiones entrantes.   
	        String deviceAso = portAssociate(deviceId("of:0000000000000001"),
				      portNumber(i+""));
		print(deviceAso);
		if(i !=6 && i!=5){		      
		    //Estado del puerto - set
		    portState(getDeviceId(deviceAso),getPortNumber(deviceAso),false);
	    	}
	    }
	    long aux2 = 0;
	    
	    /*
	    Este ciclo for cumple la funcion de inicialziar los espacios en memoria
	    Que permitiran acumular los datos a ser promediados en cada para que algortimo pueda tomar deciciones.
	    Para este caso se requiere tomar dos (puerto1 y puerto2) kp(números de datos a promediar) de muestras por cada cliente.
	    se suma un valor de uno al limite del for para tener en cuenta el corrimiento de cada
	    arreglo ya que cada uno empieza en 0 
	    */
	    for(int i = 1; i <= (Nhost_Real*5*kp+1); i++){
	        //rellena todos los arreglos con 5 ceros
		rateRx_av.add(aux);
		rateTx_av.add(aux);
		delayRx_av.add(aux);
		delayTx_av.add(aux);
	    }
	    //carga un arhivo plano con los titulos del algoritmo.
	    WriteFile("Sondeo,Device,Port,rateRx,rateTX,delayRx,delayTx,duracion,cambio",false);
	}
	
	/*
	Se vuelve falso flag para que no vuelva a inicializar las variables.
	Esto es debido a que el algorimo una vez que se ejecute en el CLI de ONOS
	siempre estara ejecutandose con las variables inicializadas en el do_execute.
	*/
	flag = false;
	
	/*
	Ciclo donde se analiza la red.	
	El valor del ciclo for es de 1000 ya que esto es aproximadamente 
	105 segundos.
	*/
	int inicio = 0;	
	float rateRxSum = 0;
	for(int i = 1; i <= 1200; i++){// tiempo de 1s
	    /*
	    cont: Acumula cada cliente/servidor por donde pasa para llevar un registro de las cuentas en orden
	    decendente.
	    */
	    int cont = 1; 
	    //Se impreme el sondeo para saber aproximadamente en que segundo va
	    print("\nSondeo=%s",i);
	    /*
	    Devuelve la lista de SW's ordenados mediante los URI(Of:of:000000000000000x) de ID de dispositivo de menor a mayor
	    para cada sondeo.
	    */
	    for(Device d : getSortedDevices(deviceService)){
	        /*
	        Se asegura que todos los SW's que estan involucrados en el algortimo realizen su funcion.
	        */
	        if(cont <= Nhost_Real){
	            /*
	            Realiza el algoritmo de volcado de trafico para el SW seleccionado 
	            */
		   rateRxSum = printPortStats(d.id(),deviceService.getPortStatistics(d.id()),cont,i);
		   if(inicio == 0){
		       if(rateRxSum > 50000){
		           print(rateRxSum+"");
		           guardaSecuencia = i;
		           print("Inicio");
		           inicio = 1;
		       }
		   }else if(inicio == 1){
		       if(rateRxSum == 0){
		           print(rateRxSum+"");
		           print("Fin");
		           portState("of:0000000000000003","1",false);
		           portState("of:0000000000000002","3",false);
		           portState("of:0000000000000002","2",false);
		           portState("of:0000000000000002","1",false);
		           portState("of:0000000000000004","1",true);
		           
		           for(int n = 1; n < rateRx_av.size(); n++){
	        		//rellena todos los arreglos con 5 ceros
				rateRx_av.set(n,(float)0);
				rateTx_av.set(n,(float)0);
				delayRx_av.set(n,(float)0);
				delayTx_av.set(n,(float)0);
	   		   }
	   		   
	   		   /*
	   		   for(int w = 0;w < pktRx_past.size();w++){
	   		   	flag_time.set(w,false);
	        		//rellena todos los arreglos con 5 ceros
				t_c.set(w,0); 
				time_past.set(w,(float)0);
				pktRx_past.set(w,(float)0);
				pktTx_past.set(w,(float)0);
				bytesRx_past.set(w,(float)0);
				bytesTx_past.set(w,(float)0);
	   		   }*/
		           cambio = 0;
		           inicio = 0;
		           guardaSecuencia = 1200;
		       }
		   }
	        }
   	        cont++;	    
	    }
	    /*
	    Espera segundos en tiempo de maquina para poder ver la captura de los datos de manera real.
	    */
	    try{
	       Thread.sleep(t_s);
	    }catch(InterruptedException ignored){
	    }
        }
        
	
    }
}
