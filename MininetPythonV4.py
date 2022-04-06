#!/usr/bin/python

# Librerias importadas

from mininet.net import Mininet
from mininet.node import Controller, RemoteController, OVSController
from mininet.node import CPULimitedHost, Host, Node
from mininet.node import OVSKernelSwitch, UserSwitch
from mininet.node import IVSSwitch
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mininet.link import TCLink, Intf
from subprocess import call

def myNetwork():
    # Se establece la clase y mascara IP
    net = Mininet( topo = None,
		   build = False,
                   ipBase='10.0.0.0/8')

    #Se agrega un controlador remoto
    info('***Adding controler \n')
    c0 = net.addController(name='c0',
			controller=RemoteController,
			ip='127.0.0.1',
			protocol='tcp',
			port=6653)

    #Se agregan switches
    info('***Adding switches\n')
    s2 = net.addSwitch('s2_host',cls=OVSKernelSwitch, protocols='OpenFlow13')
    s3 = net.addSwitch('s3_central',cls=OVSKernelSwitch, protocols='OpenFlow13')
    s1 = net.addSwitch('s1_antena',cls=OVSKernelSwitch, protocols='OpenFlow13')
    s4 = net.addSwitch('s4_central',cls=OVSKernelSwitch, protocols='OpenFlow13')
    #Se agregan hosts -sever
    info('***Adding hosts\n')
    h2 = net.addHost('h2_server', cls=Host, ip='10.0.0.2', defaultRoute=None)
    h1 = net.addHost('h1_host', cls=Host, ip='10.0.0.1', defaultRoute=None)

    #Se define anchos de banda para los enlaces
    info('*** Add links\n')
    Link1 = {'bw':2}
    Link2 = {'bw':4}
    Link3 = {'bw':6}
    
    # (SWITCH - SWITCH) agregando el ancho de banda deseado
    net.addLink(s1, s2, cls=TCLink, **Link1)
    net.addLink(s1, s2, cls=TCLink, **Link2)
    net.addLink(s1, s2, cls=TCLink, **Link3)
    # (SWITCH - SWITCH)
    net.addLink(s1,s3)
    net.addLink(s1,s4)
    # (SWITCH - SWITCH)
    net.addLink(s2,s3)
    net.addLink(s2,s4)
    #(SWITCH - SERVER)
    net.addLink(s1, h1)
    #(SWITCH - SERVER)
    net.addLink(s2, h2)
    
    info( '*** Stating network\n')
    net.build()
    info( '*** Starting controllers\n')
    for controller in net.controllers:
        controller.start()

    #Se establece una conexion entre el controlador y los switches
    info('*** Starting switches\n')
    net.get('s4_central').start([c0])
    net.get('s3_central').start([c0])
    net.get('s1_antena').start([c0])
    net.get('s2_host').start([c0])

    info( '*** Post configure switches and hosts\n')

    CLI(net)
    net.stop()

if __name__ == '__main__':
    setLogLevel('info')
    myNetwork()
