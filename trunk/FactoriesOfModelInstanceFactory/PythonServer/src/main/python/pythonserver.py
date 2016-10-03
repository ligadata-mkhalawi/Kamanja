#!/usr/bin/python
# server.py
import socket
import time
import argparse
import sys
import hashlib
import struct
import json
import logging
import logging.config
import logging.handlers
import os
import os.path
import select


#
#
parser = argparse.ArgumentParser(description='Kamanja PyServer')
parser.add_argument('--host', help='host name (default=localhost) ', default="localhost", type=str)
parser.add_argument('--port', help='port binding (default=9999)', default="9999",type=int)
parser.add_argument('--pythonPath', required=True, type=str)
parser.add_argument('--log4jConfig', required=True, type=str)
parser.add_argument('--fileLogPath', default="", type=str)
parser.add_argument('--pkey', default="", type=str)
args = vars(parser.parse_args())
#
#
# set the sys.path s.t. the first path searched is our path
pythonPath = args['pythonPath']
sys.path.insert(0, args['pythonPath'])
#
#
#
partitionkey = args['pkey'] 
#set python buffering
if sys.version_info[0] == 3:
   os.environ['PYTHONUNBUFFERED'] = '1'
#
# initialize logging
log4jConfig = args['log4jConfig']
logging.config.fileConfig(log4jConfig)
# create logger
logger = logging.getLogger('pythonserver')
# add file logger if specified on command line
fileLogPath = args['fileLogPath']
fileLogPath += str(os.getpid())
if fileLogPath != "":
	fileHandler = logging.handlers.RotatingFileHandler(fileLogPath, mode='a', maxBytes=500000, backupCount=3)
#        fileHandler = logging.StreamHandler()
	formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
	fileHandler.setFormatter(formatter)
	logger.addHandler(fileHandler)

# log basic startup info
if logger.isEnabledFor(logging.INFO): 
   logger.info('starting pythonserver ...\nhost = ' + args['host'] + '\nport = ' + str(args['port']) + '\npythonPath = ' + args['pythonPath'] + '\nlog4jConfig = ' + args['log4jConfig'])
#


CONNECTION_LIST = []    # list of socket clients
RECV_BUFFER = 4096 # Advisable to keep it as an exponent of 2
# create a socket object
serversocket = socket.socket(
	        socket.AF_INET, socket.SOCK_STREAM)
serversocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
#
# get local machine name
host = '' #listen to whatever the localhost is called .... socket.gethostname()
hostDisplayStr = socket.gethostname()
port = int(args['port'])
#
# bind to the port
if logger.isEnabledFor(logging.INFO): 
   logger.info('Connection parms: ' + hostDisplayStr + ", " + str(args['port']))
serversocket.bind((host, port))
#
def importPackageByName(moduleName, name):
	'''
	Import the class 'name' found in package 'moduleName'.  The moduleName
	may be a sub-package (e.g., the module is found in the 'commands'
	sub-package in the use below).
	'''
	try:
		if logger.isEnabledFor(logging.INFO):
                   logger.info("load moduleName = " + moduleName)
		module = __import__(moduleName, globals(), locals(), [name])
	except ImportError:
		return None
	return getattr(module, name)
#
# command dictionary used to manage server loaded below
cmdDict = dict()
#
# model instance dictionary.
modelDict = dict()
# toss the pypath into the modelDict so it can be discovered by the addModel
modelDict["PythonInstallPath"] = args['pythonPath']
#
# Modify command dict with the python server commands supported.
# These are loaded on the server from the 'commands' sub directory specified
# of a directory on the PYTHONPATH...e.g.,
#
#	export PYTHONPATH=$KAMANJA_HOME/python/
#
# Using the PYTHONPATH makes sense at least for testing.  It might be fruitful
# for production as well...
#
# A similar approach is used for the model dispatch dictionary.  It is dynamically
# created from content, however, that has been sent to the server on an open
# connection to the server.

# import the base model classes that are in the common subdirectory
#from abc import ABCMeta

importPackageByName("common.ModelBase", "ModelBase")
importPackageByName("common.ModelInstance", "ModelInstance")
# Add the system level command to the dispatcher dict
for moduleName in 'addModel', 'removeModel', 'serverStatus', 'executeModel', 'stopServer':
	pkgCmdName = "commands.{}".format(moduleName)
	className = moduleName # also
	HandlerClass = importPackageByName(pkgCmdName, className)
        if logger.isEnabledFor(logging.INFO): 
	   logger.info("load HandlerClass({},{},{})".format(pkgCmdName,hostDisplayStr,str(port)))
	handler = HandlerClass(pkgCmdName, hostDisplayStr, port, logger)
	cmdDict[moduleName] = handler
#self, modelDict, host, port, cmdOptions, modelOptions
startMarkerValue = "_S_T_A_R_T_"
endMarkerValue = "_F_I_N_I_"
crcValueLen = 8

if logger.isEnabledFor(logging.DEBUG):
   logger.debug("cmds in cmdDict = {}, len = {}".format(str(cmdDict.keys),len(cmdDict)))
#
def nextMsg(conn, msgBytes):
    '''
    Assumption: more than one message can be received before a reply is made.
	Description: Find the component parts for the next message (startMark,
	checksum, cmdMsgLen, cmdMsg, endMark) and preserve any residual bytes for
	consideration for the next message.
    '''
    if logger.isEnabledFor(logging.DEBUG):
       logger.debug("entering nextMsg...msgBytes = {}".format(str(msgBytes)))
       logger.debug("calling startMsg(conn, {},{})".format(startMarkerValue, str(msgBytes)))
    #
    (startMsgMark, checksum, cmdLen, followingBytes) = startMsg(conn, startMarkerValue, msgBytes)
    #
    if logger.isEnabledFor(logging.DEBUG): 
       logger.debug("startMsg results = ({},{},{},{})".format(startMsgMark, checksum, cmdLen, str(followingBytes)))
    lenOfFollowingBytes = len(followingBytes)
    if logger.isEnabledFor(logging.DEBUG): 
       logger.debug("lenOfFollowingBytes = {}".format(lenOfFollowingBytes))
       logger.debug("calling completeMsg(conn, {},{},{})".format(endMarkerValue, cmdLen, str(followingBytes)))
	#
    (cmdMsg, endMsgMark, nextMsgBytes) = completeMsg(conn, endMarkerValue, cmdLen, followingBytes)
    #
    if logger.isEnabledFor(logging.DEBUG): 
       logger.debug("completeMsg results = ({},{},{})".format(startMsgMark, checksum, cmdLen, str(followingBytes)))
	#
    cmdMsgDict = json.loads(cmdMsg)
    prettycmd = json.dumps(cmdMsgDict, sort_keys=True, indent=4, separators=(',', ': '))
    if logger.isEnabledFor(logging.DEBUG): 
       logger.debug("cmd = \n{}".format(prettycmd))
    #
    return cmdMsgDict
#
def startMsg(conn, startMarker, msgBytes):
	'''
	Obtain the starting FIXED portion of the current msg in the recv.
	Receive bytes until a complete fixed portion may be considered.
	Parameters are the connection, the startMarker value sought and any
 	msgBytes that may be left from a prior receive.
	Answer from receive buffer the fixed part of the message... namely
	the (startMark, checksum, cmdMsgLen).
	Note: the assumption is that more than one message can be sent before
	a message is processed.
	'''
	#
	# return values
	#
	startMarkerValue = ""
	crc = 0L
	payloadLen = 0
	followingBytes = b""
	#
	# working variables
	#
	fixedMsgPortion = "" #fragment of cmd msg collected here and values extracted from it
	chunks = [] #received chunks from conn collected here and joined
	chunks.append(msgBytes) #received unprocessed bytes from previous recv are first
	bytes_recd = len(msgBytes) #acknowledge that prior bytes are already available
	# constant values that describe the lengths of the three pieces of info sought
	lenStartMarker = len(startMarker)
	lenOfWireInt = 4 # cmdSize is 4 byte int
	lenOfCheckSum = 8 # checksum into a 8 byte int
	# calculate the number of bytes needed before evaluate and extract
	bytesNeeded = len(startMarker) + lenOfCheckSum + lenOfWireInt - len(msgBytes)
	#
	#
	if bytesNeeded > 0:
                if logger.isEnabledFor(logging.DEBUG): 
		   logger.debug("bytesNeeded are {}".format(bytesNeeded))
		# get the additional bytes needed to satisfy the beg mark, crc, and len requisition
		while bytes_recd < bytesNeeded:
			chunk = conn.recv(min(bytesNeeded - bytes_recd, 8192))
			if chunk == '':
				raise RuntimeError("socket connection broken")
			chunks.append(chunk)
			bytes_recd = bytes_recd + len(chunk)
	#
	rawmsg =  b"".join(chunks)
	startMarkerBase = 0
	try:
		startMarkerBase = rawmsg.index(startMarker)
	except ValueError:
		logger.error('from connection bytes "{}", start marker "{}" was not found'.format(rawmsg, startMarker))
		logger.error('giving up... something is fouled')
		raise
	if startMarkerBase != 0: #this is not good... there should be no
		# slack bytes between cmdMsgs in received bytes.
                if logger.isEnabledFor(logging.DEBUG): 
		   logger.debug('there is junk residual found before the start marker... value of junk = "{}"'.format(rawmsg[0:startMarkerBase]))
		raise BufferError('there is junk residual found before the start marker... value of junk = "{}"'.format(rawmsg[0:startMarkerBase]))
	#
	fixedMsgPortion = rawmsg[startMarkerBase:]
	# slice up the rawmsg; treat scalars appropriately and then collect the return
	# values ... the start marker. the crc value and the cmdSize. The rest is returned
	# as followingBytes... part of a following message... it is preserved by the caller
	# and presented when the next message is considered.
	begMark = 0
	endMark = lenStartMarker
	startMarkerValue = fixedMsgPortion[begMark:endMark]
	begMark = endMark
	endMark += lenOfCheckSum
	crcBytes = fixedMsgPortion[begMark:endMark]
	# big endian unsigned long long (8 bytes)
	(crc,) = struct.unpack('>Q', bytearray(crcBytes))
	#
	begMark = endMark
	endMark = endMark + lenOfWireInt
	payloadLenBytes = fixedMsgPortion[begMark:endMark]
	(payloadLen,) = struct.unpack('>I', bytearray(payloadLenBytes)) #big endian unsigned int (4 bytes)
	#
        if logger.isEnabledFor(logging.DEBUG): 
	   logger.debug("startMsg() extracted fixed part = {}, {}, {}".format(startMarkerValue, crc, payloadLen))
	#
	begMark = endMark
	followingBytes=fixedMsgPortion[begMark:]
	#
        if logger.isEnabledFor(logging.DEBUG): 
	   logger.debug("startMsg() ...followingBytes = {}, {}, {}".format(str(followingBytes), begMark, (len(fixedMsgPortion) - begMark)))
	#
	return (startMarkerValue, crc, payloadLen, followingBytes)
#
def completeMsg(conn, endMarkerValue, cmdLen, residualBytesLastRead):
	'''
	Called after startMsg, the bytesReceived by the startMsg are searched first
	for the endMarker.  If not found, additional bytes are retrieved from the
	connection until it is found.  Any residual bytes found after the end marker
	are considered part of the next command message.
	This function will obtain the command message bytes, the end tag, and any
	residual bytes that may be part of a subsequent command.
	Answer the tuple, (cmd msg, end tag, the next message bytes buffer)
	'''
        if logger.isEnabledFor(logging.DEBUG): 
	   logger.debug("entered completeMsg(conn, {},{},{})".format(endMarkerValue, cmdLen, residualBytesLastRead))
	#
	# return values
	#
	cmdMsg = b""
	endMsgMark = ""
	followingBytes = residualBytesLastRead
	#
	# working variables
	#
	rawmsg = bytearray()
	bytes_recd = len(residualBytesLastRead)
	if bytes_recd > 0:
		rawmsg.extend(residualBytesLastRead)
	markerFound = True #optimism
	lenEndMarker = len(endMarkerValue)

	# calculate the number of bytes needed before evaluate and extract
	bytesNeeded = lenEndMarker + cmdLen - len(residualBytesLastRead)
        if logger.isEnabledFor(logging.DEBUG): 
	   logger.debug("completeMsg(bytesNeeded = {})".format(bytesNeeded))
	#
	if bytesNeeded > 0:
		# get the additional bytes (if needed) to satisfy the
		# beg mark, crc, and len requisition
		while bytes_recd < bytesNeeded:
			chunk = conn.recv(min(bytesNeeded - bytes_recd, 8192))
			if chunk == '':
				raise RuntimeError("socket connection broken")
			rawmsg.extend(chunk)
                        if logger.isEnabledFor(logging.DEBUG): 
			   logger.debug("bytes_recd = {} < bytesNeeded = {}".format(bytes_recd,bytesNeeded))
			bytes_recd = bytes_recd + len(chunk)
	#
	endMarkerBase = 0
	try:
		endMarkerBase = rawmsg.index(endMarkerValue)
	except ValueError:
		logger.error('completeMsg()...from connection bytes ({}) received that are of sufficient size to contain the cmdMsg and end marker, end marker "{}" was not found'.format(rawmsg, endMarkerValue))
		logger.error('giving up... something is fouled')
		raise
	#
	# get the cmdMsg bytes and end marker value from the enclosed bytes, leaving residual
	# in the followingBytes which purportedly are part of a subsequent message in the
	# input.
	cmdMsg = str(rawmsg[0:endMarkerBase])
	mark = endMarkerBase + lenEndMarker
	endMsgMark = str(rawmsg[mark:lenEndMarker])
	followingBytes=rawmsg[mark:]

	if logger.isEnabledFor(logging.DEBUG):
           logger.debug("completeMsg() ... cmdMsg = {}, endMsgMark = {}, followingBytes = {}".format(cmdMsg,endMsgMark,str(followingBytes)))
	#
	return (cmdMsg, endMsgMark, followingBytes)
#
def formatWireMsg(msg):
	"""
	Format the supplied message for transmission to client.  Format is
	described in the dispatcher function.
	"""
	if logger.isEnabledFor(logging.DEBUG):
           logger.debug("formatWireMsg msg to package = '{}'".format(str(msg)))
	reply = bytearray()
	reply.extend(startMarkerValue)		# start marker for all messages
	reply.extend(struct.pack('>Q', 0)) 	# crc (unused)
	msgBytesLen = len(msg)
	reply.extend(struct.pack('>I', msgBytesLen)) 	# msg content length
	reply.extend(msg) 					# msg content
	reply.extend(endMarkerValue)			# end marker for all messages

	if logger.isEnabledFor(logging.INFO):
           logger.info("formatWireMsg reply = {}".format(str(reply)))
        if logger.isEnabledFor(logging.DEBUG): 
	   logger.debug("formatWireMsg reply length = {}".format(len(reply)))
        sys.stdout.flush()
        sys.stderr.flush()
	return reply
#
def goingDownMsg():
	"""
	A stop server command has been received.  Format a standard message and
	return it
	"""
	svrdownMsg = json.dumps({'Server' : hostDisplayStr, 'Port' : str(port), 'Result' : "python server stopped by user command"})
 	rawmsg = formatWireMsg(svrdownMsg)
	return rawmsg
#
def exceptionMsg(infoTag):
	"""
	print failure locally
	format the failure as json dict
	build a wire message for transmission to client
	answer formatted message
	Note: it is also possible to get traceback info but it is quite involved... perhaps
	another time.
	"""
	prettycmd = json.dumps({'Server' : hostDisplayStr, 'Port' : str(port), 'Result' : infoTag, 'Exception' : str(sys.exc_info()[0]), 'FailedClass' : str(sys.exc_info()[1])}, sort_keys=True, indent=4, separators=(',', ': '))
        if logger.isEnabledFor(logging.DEBUG): 
	   logger.debug(prettycmd)
	xeptMsg = json.dumps({'Server' : hostDisplayStr, 'Port' : str(port), 'Result' : infoTag, 'Exception' : str(sys.exc_info()[0]), 'FailedClass' : str(sys.exc_info()[1])})
 	rawmsg = formatWireMsg(xeptMsg)
	return rawmsg
#
#cmd = None
def dispatcher(cmdMsgDict):
	"""
	The message byte array passed to and fro has these segments:
	<StartMarker>, <crc>, <DataLen>, <JsonData>, <EndMarker>
	b.  The <DataLen> is a 4 byte representation of the length of the
	<JsonData> string.
	c.  The crc is currently unused but will hold some modest hash on msg content
	in future release
	d.  The <JsonData> itself is a map.  In addition to the message integrity checks, to be successfully processed, a key must be found in the map == 'cmd' whose value is the command that will interpret the remaining key/value pairs in the dictionary.  Should the 'cmd' key value not refer to a
	legitimate command,  error is logged and returned.
	"""
	#handler(self, modelDict, host, port, cmdOptions, modelOptions)
	if "CmdOptions" in cmdMsgDict:
		cmdOptions = cmdMsgDict["CmdOptions"]
	else:
		cmdOptions = []
	if "ModelOptions" in cmdMsgDict:
		modelOptions = cmdMsgDict["ModelOptions"]
	else:
		modelOptions = []

	cmdNameView = cmdDict.viewkeys()
	cmdNames = ["{}".format(v) for v in cmdNameView]
	cmdValuesView = cmdDict.viewvalues()
	cmdInsts =  ["{}".format(str(v)) for v in cmdValuesView]
        if logger.isEnabledFor(logging.DEBUG): 
	   logger.debug("cmds in cmdDict = {}, len = {}".format(cmdNames,len(cmdDict)))
	   logger.debug("instances in cmdDict = {}, len = {}".format(cmdInsts,len(cmdDict)))

	results = ""
	try:
		cmdkey = cmdMsgDict["Cmd"]
	except:
		results = exceptionMsg('The command key "Cmd" was not found in the supplied message')

	if results == "":
		try:
			cmdDesired = cmdMsgDict["Cmd"]
                        if logger.isEnabledFor(logging.DEBUG): 
			   logger.debug("cmdDesired = {}".format(cmdDesired))
			cmd = cmdDict.get(cmdDesired)
                        if logger.isEnabledFor(logging.DEBUG): 
			   logger.debug("cmd instance = {}".format(str(cmd)))
			results = cmd.handler(modelDict, hostDisplayStr, port, cmdOptions, modelOptions)
		except:
			results = exceptionMsg("The command '{}' is having a bad day...".format(cmdkey))

	return results
#
################################################################################
# socket connection loop...
#
# queue up to 5 requests... could be another value... as written
# the conn are serialized... we mitigate by having a pythonserver for each
# partition the engine chooses to distribute the input with.
#
if logger.isEnabledFor(logging.INFO):
   logger.info("begin listening for connections...")

parentpid=os.getppid()
serversocket.listen(10)
# Add server socket to the list of readable connections
CONNECTION_LIST.append(serversocket)
result = ''
msgBytes = ''
while True:
   # Get the list sockets which are ready to be read through select
   read_sockets,write_sockets,error_sockets = select.select(CONNECTION_LIST,[],[], 0.1)
   for sock in read_sockets:
        if (sock == serversocket):
                # establish a connection
                if logger.isEnabledFor(logging.INFO): 
	           logger.info('accepting connections...')
	        conn, addr = serversocket.accept()
                CONNECTION_LIST.append(conn)
                conn.setblocking(0)
                if logger.isEnabledFor(logging.DEBUG): 
	           logger.debug('Connected by', str(addr))
	else:
		closedConnection = False
		exceptionOccurred = False
		try:
			cmdMsgDict = nextMsg(sock, msgBytes)
		except:
			exceptionType = sys.exc_info()[0]
			classFailed = sys.exc_info()[1]
			callback = sys.exc_info()[2]
			emsg = "nextMsg exception...\ntype = {},\nclass = {},\nstack = {}".format(exceptionType,classFailed,callback)
			if logger.isEnabledFor(logging.DEBUG):
                            logger.debug(emsg)
			wireMsg = formatWireMsg(emsg)
			sock.sendall(wireMsg)
			sys.exc_clear()
			exceptionOccurred = True
                        sock.close()
                        CONNECTION_LIST.remove(sock)
			closedConnection = True
		#
		if exceptionOccurred == False and closedConnection == False:
			try:
				result = dispatcher(cmdMsgDict) # json string answers except stopServer
                                if (logger.isEnabledFor(logging.DEBUG)):
                                   logger.debug ("This is the first place for dispatcher command " + result) 
			except:
				wireMsg = exceptionMsg("dispatched cmd exception...")
                                if (logger.isEnabledFor(logging.DEBUG)):
                                    logger.debug ("This is place exception occurs  " + exceptionMsg) 
				sock.sendall(wireMsg)
				sys.exc_clear()
				exceptionOccurred = True
				sock.close()
                                CONNECTION_LIST.remove(sock)
				closedConnection = True
			if exceptionOccurred == False and closedConnection == False:
				# result
				try:
				    if result != 'kill-9': # stop command will return 'kill-9' as value
						wireMsg = formatWireMsg(result)
                                                if (logger.isEnabledFor(logging.DEBUG)):
                                                      logger.debug ("This is the where result sent  " + result) 
						sock.sendall(wireMsg)
				    else:
						wireMsg =  goingDownMsg()
					        sock.sendall(wireMsg)
				except:
                                        if (logger.isEnabledFor(logging.DEBUG)):
                                           logger.debug ("This is the exception place second  " + result) 
					logger.error("the connection has been broken...closing connection")
					sock.close()
                                        CONNECTION_LIST.remove(sock)
					closedConnection = True
		if closedConnection:
			break
		if result == 'kill-9': # stop command stops listener tears down server ...
			break
	if result == 'kill-9': # stop command stops listener tears down server ...
		break

   if (parentpid != os.getppid()):
      logger.info("parent pid exited so hence child exiting")
      os._exit(0)
#   else:
#      logger.debug("")
#
if logger.isEnabledFor(logging.INFO): 
   logger.info('server stopped by admin command')
