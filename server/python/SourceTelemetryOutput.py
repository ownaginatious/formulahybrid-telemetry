'''
Created on Apr 2, 2014

@author: dillondixon
'''
import socket
import struct
import uuid
from enum import IntEnum
import threading
import queue
import ControlFlags
from ControlFlags import ClientCommands
import os

class SourceTelemetryOutput(object):
    '''
    classdocs
    '''
    
    __responses = IntEnum('Response', 'IDINUSE NOSUCHLOG INCOMINGLOG OK')
    PROTOCOL_HEADER = "CTSD".encode(encoding='utf_8')
    
    #FIXME: Find actual standard port.
    PROTOCOL_STANDARD_PORT = 8888;
    
    def __init__(self, source_name, log_path):
        '''
        Constructor
        '''
        
        self.source_name = source_name.encode(encoding='utf_8')
        
        # Connections that can be thrown while listening to a feed (definition for relays).
        self.throwables = set()
        
        # Connections receiving the telemetry feed.
        self.feeding_connections = set()
        self.standby_connections = set()
        self.log_path = log_path
        
        # Map of connection IDs to connections.
        self.connections = dict()
        
        # Buffered binary blob of connected clients.
        self.buffered_connections_binary = b''
        self.send_lock = threading.Lock()
        self.last_com = dict()
        
        # Queues for adding and removing connections at regular intervals.
        self.add_queue = queue.Queue
        self.remove_queue = queue.Queue
        
        self.add_to_feed = queue.Queue
        
        # Refresh the list of logs.
        self.refresh_logs();
        
        # Create the listener thread.
        
        # Create sweep thread.
        
    def __server_socket_routine(self):
        
        # Create the listener socket.
        
        ss = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        ss.bind((socket.gethostname(), self.PROTOCOL_STANDARD_PORT));
        ss.listen(5) # We will allow for up to 5 queued grouping_connections.
        
        while True:
            
            try:
                
                # Accept a new socket connection.
                connection, address = ss.accept()
                print("Connection from [" + address + "] incoming.")
                
                # The longest we are willing to wait on this connection for a
                # response is 1 second.
                connection.settimeout(1)
                
                # Send the protocol header.
                connection.send(self.PROTOCOL_HEADER)
                
                # Send the name of the source.
                connection.send(struct.pack("b", len(self.source_name) & 0xff));
                connection.send(self.source_name);
                
                # Read back the ID of the source.
                uuid_bytes = connection.recv(16)
                incoming_uuid = uuid.UUID(bytes=uuid_bytes)
            
                # If someone is already connected under this ID, tell the connecting
                # client and disconnect from it. 
                if incoming_uuid in self.grouping_connections.union(self.groupless_connections):
                    
                    connection.send(struct.pack("b", self.__responses.IDINUSE & 0xff))
                    connection.close()
                    continue
                
                # Add the connection to the binary blob.
                self.buffered_connections_binary += uuid_bytes
                
                self.connections[incoming_uuid] = connection
                self.standby_connections[incoming_uuid] = connection
                self.last_com[incoming_uuid] = 0
                        
                connection.send(struct.pack("b", self.__responses.OK & 0xff))
                
                # The thread should now be non-blocking.
                connection.settimeout(0)
                
            except socket.timeout:
                
                print("Incoming connection timed out and was aborted.")
                
            except ConnectionAbortedError:
                
                print("Server connection closed. Shutting down.")
                break # The server was closed and the connection is terminating.
        
    def __sweep_response_routine(self):
        '''
        Routinely sweeps through the feed connections and checks for heart beats. Also 
        adds new clients to connection pool and feed pool.
        '''
        
        # Lock the sending connections.
        with(self.send_lock):
            
            stop_feed = set()
            
            for x in self.feeding_connections:
                
                if not x in self.connections:
                    continue
                
                connection = self.connections[x]
                
                signalled_true = False
                
                try:
                    
                    # Clear the buffer for this connection.
                    while True:
                        
                        # Try and receive a heart beat flag from the buffer.
                        flag_input = connection.recv(len(ControlFlags.STANDARD_CONTROL_FLAG_SIZE))
                    
                        # The call is non-blocking
                        if flag_input == ControlFlags.HEART_BEAT_CONTROL_FLAG:
                            signalled_true = True
                        elif flag_input == ControlFlags.STREAM_STOP_CONTROL_FLAG:
                            stop_feed += {x}
                            break
                        else: # Client signaled something unexpected, and should be terminated.
                            self.remove_queue.put_nowait(x)
                            break
                        
                except socket.timeout:
                    
                    if not signalled_true:
                        self.remove_queue.put_nowait(x);
            
            # Remove and confirm with clients who have requested to terminate their feeds.
            for x in stop_feed:
                
                self.feeding_connections.remove(x);
                
                if x in self.throwables:
                    self.throwables.remove(x)
                
                # Repeat the stop-stream flag to the client to let them know the stream
                # to them has successfully stopped.
                self.connections[x].send(ControlFlags.HEART_BEAT_CONTROL_FLAG)
            
            # Add new connections to the feed, and send them a heart beat to begin the stream.
            while not self.add_to_feed.empty():
                
                new_connection, throwable = self.add_to_feed.get_nowait()
                self.add_to_feed(new_connection)
                self.throwables.add(throwable)
                self.connections[new_connection].send(ControlFlags.HEART_BEAT_CONTROL_FLAG)
    
    def __mirror_command(self, connection, command):
        connection.send(struct.pack("!b", command & 0xff))
        
    def __command_handler_routine(self):
        '''
        Handle commands immediately. None of the commands should have long enough execution times as to be blocking.
        '''
        
        # Scan each standby socket for incoming commands.
        for x in self.connections:
            
            connection = self.connections[x]
            
            try:
                
                command = connection.recv(1)
                self.last_com[x] = 0
                
                try:
                    if command >= 0 and command < ClientCommands.SIZE:
                        
                        # Mirror command for acknowledgement.
                        self.__mirror_command(connection, command)
                            
                        if command == ClientCommands.GETOTHERS:
                            
                            # Get ID buffer length and send it (limited to 255)
                            connection.send(struct.pack("!b", (len(self.buffered_connections_binary)/16) & 0xff))
                            
                            # Send the binary data.
                            connection.send(self.buffered_connections_binary)
                            
                        elif command == ClientCommands.STARTFEED:
                            
                            throwable = connection.recv(1) != 0
                            self.add_to_feed.put_nowait((x, throwable))
                            
                        # If this command is being received here, it can be ignored.
                        elif command == ClientCommands.STOPFEED:
                            pass
                        
                        # Add the connection to those to be removed.
                        elif command == ClientCommands.DISCONNECT:
                            self.remove_queue.put_nowait(x)
                        
                        # Return all the logs on this system.
                        elif command == ClientCommands.GETLOGS:
                            connection.send(self.buffered_log_binary);
                            
                        elif command == ClientCommands.RETRIEVELOG:
                            pass
                        elif command == ClientCommands.RETRIEVELOGFEED:
                            pass
                        else:
                            print("Something wrong with ClientCommands enum. Please review it.")
                            print("Dropping connection " + x + ", as it requested something technically unknown. [Command : " + command + "]")
                            self.remove_queue.put_nowait(x)
                            
                    else:
                        
                        print("Unrecognized command from client " + x + ". Dropping Connection.")
                        self.remove_queue.put_nowait(x)
                        
                except socket.timeout:
                    
                    print("Client " + x + " failed to complete its command. Dropping Connection.")
                    self.remove_queue.put_nowait(x)
                    
            except socket.timeout:
                
                self.last_com[x] += 1
                continue;
        
    def send_message_to_clients(self):
        pass
        
    def refresh_logs(self):
        
        # Start going through only the files at the path.
        files = [f for f in os.listdir(self.log_path) if os.path.isfile(self.log_path + f)]
        
        # Filter out files with names longer than 255 bytes and cut the yield to only 255 files.
        files = [x.encode("utf-8") for x in files[0:255] if len(x.encode("utf-8")) <= 255]
        
        self.buffered_log_binary = struct.pack("!B", len(files))
        
        for x in files:
            
            self.buffered_log_binary = struct.pack("!B", x & 0xff)
            self.buffered_connections_binary += x