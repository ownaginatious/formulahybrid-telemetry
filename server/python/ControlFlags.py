'''
Created on Apr 3, 2014

@author: dillondixon
'''
import struct

PROTOCOL_HEADER = "TCF".encode(encoding='utf_8')

STANDARD_CONTROL_FLAG_SIZE = len(PROTOCOL_HEADER) + 2;

HEART_BEAT_CONTROL_FLAG = PROTOCOL_HEADER + struct.pack("!h", 0x01)
STREAM_STOP_CONTROL_FLAG = PROTOCOL_HEADER + struct.pack("!h", 0x02)

class ClientCommands(object):
    
    GETOTHERS = 0
    STARTFEED = 1
    STOPFEED = 2
    DISCONNECT = 3
    GETLOGS = 4
    RETRIEVELOG = 5
    RETRIEVELOGFEED = 6
    
    # Size of enumeration.
    SIZE = 7