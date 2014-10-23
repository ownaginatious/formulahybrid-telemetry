'''
Created on Apr 1, 2014

@author: dillondixon
'''

import socket
import struct
import queue
import threading

class CANTransmitter(object):
    '''
    classdocs
    '''
    
    # CAN frame packing/unpacking (see `struct can_frame` in <linux/can.h>)
    __can_frame_format = "=IB3x8s"
    __buffer = queue.Queue(0) # Infinitely large synchronized queue.
    __buffering = False
    __write_lock = threading.Lock
    
    def __init__(self, port_id):
        '''
        Constructor
        '''
        
        # create a raw socket and bind it to the given CAN interface
        self.s = socket.socket(socket.AF_CAN, socket.SOCK_RAW, socket.CAN_RAW)
        self.s.bind((port_id,))

    def __build_can_frame(self, can_id, data):
        
        can_dlc = len(data)
        data = data.ljust(8, b'\x00')
        return struct.pack(self.__can_frame_fmt, can_id, can_dlc, data)
 
    def __dissect_can_frame(self, frame):
        
        can_id, can_dlc, data = struct.unpack(self.__can_frame_fmt, frame)
        return (can_id, can_dlc, data[:can_dlc])
    
    def __buffer_thread_routine(self):
        
        with self.__write_lock:
            while self.__buffering:
                
                # Receive the next incoming CAN network message.
                cf, _ = self.s.recvfrom(16) # Receiving 16-bits of data.
                self.__buffer.put_nowait(self.__dissect_can_frame(cf))
    
    def begin_buffering(self):
        
        if self.__buffering:
            return
        
        self.__buffering = True
        
        threading.Thread(target=self.__buffer_thread_routine())
    
    def stop_buffering(self):
        
        self.__buffering = False
        
        # Wait for the thread to die.
        with self.__write_lock:
            pass
    
    def disconnect(self):
        self.s.close();
    
    def get_message(self):
        return self.__buffer.get_nowait()
    
    def send_message(self, can_id, data):
        self.s.send(self.__build_can_frame(can_id, data))