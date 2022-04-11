#!/bin/bash
activarFooapp(){
    sshpass -p "karaf" ssh -p 8101 karaf@192.168.0.37 "app activate org.foo.app"
}
desactivarFooapp(){
    sshpass -p "karaf" ssh -p 8101 karaf@192.168.0.37 "app deactivate org.foo.app"
}
echo "desactivo FooApp" 
desactivarFooapp
cd foo-app
echo "cambia a la ruta foo-app"
pwd
echo "compila el AppCommand.java"
mvn clean install -Dcheckstyle.skip
cd
echo "cambia a la ruta personal"
pwd
echo "source onos/tools/dev/bash_profile"
cd foo-app
echo "$cambia a la ruta foo-app"
pwd
echo "reisntala la aplicacion"
onos-app localhost reinstall! target/foo-app-1.0-SNAPSHOT.oar
echo "Activa FooApp"
activarFooapp
