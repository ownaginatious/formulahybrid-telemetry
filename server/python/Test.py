'''
Created on Apr 27, 2014

INSTRUCTIONS:
-------------
First enable the dcan1 port on the BeagleBone by running the following
as root in the terminal before running this program.

    ip link set can0 up type can bitrate 500000
    ifconfig can0 up
    
This program will continuously print incoming CAN messages in binary.

@author: dillondixon
'''

# Not sure what the CAN port directory is. Someone check.
CAN_PORT = '/dev/dcan1'

from CANTransmitter import CANTransmitter

t = CANTransmitter(CAN_PORT)

t.begin_buffering();

while True:
    
    can_id, can_dlc, data = t.get_message()
    print("ID: " + "{0:b}".format(can_id) + ", LENGTH: " + can_dlc + ", DATA: " + "{0:b}".format(data))