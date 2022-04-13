#!/bin/bash
dbus-launch gnome-terminal -x bash -c "
gnome-terminal -x bash -c \"echo \"savewow\" | sudo -S $HOME/onos/bin/onos-service start; exec $SHELL\";
gnome-terminal -x bash -c \"sleep 120;export XAUTHORITY=$HOME/.Xauthority;cd MininetPython;echo \"savewow\" | sudo -S echo;sudo python3 MininetPythonV4.py; exec $SHELL\";
gnome-terminal -x bash -c \"cd foo-app/src/main/java/org/foo/app/;gedit ~/AppCommandCopiaOptimizada/AppCommand.java AppCommand.java; exec $SHELL\"; 
gnome-terminal -x bash -c \"sleep 60;sshpass -p \"karaf\" ssh -p 8101 karaf@192.168.0.37; exec $SHELL\";
gnome-terminal -x bash -c \"source onos/tools/dev/bash_profile; exec $SHELL\";
exit;
exec $SHELL" &










