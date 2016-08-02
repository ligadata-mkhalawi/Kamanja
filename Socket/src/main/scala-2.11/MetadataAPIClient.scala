/**
  * Created by Samer-Work on 31-07-2016.
  */
package com.ligadata

import java.io.{BufferedReader, IOException, InputStreamReader, PrintWriter}
import java.net.{Socket, UnknownHostException}
import java.util.logging.LogManager

import com.ligadata.MetadataAPI.Utility.SocketCommunicationHelper
import org.apache.logging.log4j.LogManager

/**
  * Created by Yasser on 6/7/2016.
  */
object MetadataAPIClient {

  private val logger = LogManager.getLogger(getClass)

  //split on the comma only if that comma has zero, or an even number of quotes ahead of it.
  def split(str : String, separator : String) : Array[String] = {
    val tokens = str.split(separator + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1)
    tokens.filter(token => token.length > 0).toList.toArray
  }

  def main(args : Array[String]): Unit ={

    var hostName = ""
    var portNumber = -1

    var cmdArgs = Array[String]()

    //TODO : pas params as arguments for now, better use a config file
    if(args.length >= 2) {
      hostName = args(0)
      portNumber = args(1).toInt
    }
    if(args.length > 2){
      cmdArgs = args.slice(2, args.length)//take rest of params
    }

    if(isKamanjaLocal){
      logger.info("Running Metadata API Client on local machine")
      connectToLocal(hostName, portNumber, cmdArgs)
    }
    else{
      logger.info("Running Metadata API Client remotely")
      connectToRemote(hostName, portNumber)
    }

  }

  def isKamanjaLocal: Boolean ={
    true;//TODO : implement this
  }

  /**
    * connect using rest api
    * @param hostName
    * @param portNumber
    */
  def connectToRemote(hostName : String, portNumber : Int) : Unit = ???

  /**
    * connect using socket connection
    *
    * @param hostName
    * @param portNumber
    */
  def connectToLocal(hostName : String, portNumber : Int, cmdArgs : Array[String]): Unit ={

    var socket : Socket = null
    try {
      socket = new Socket(hostName, portNumber)
      val out = socket.getOutputStream
      val in = socket.getInputStream
      val stdIn = new BufferedReader(new InputStreamReader(System.in))

      if(cmdArgs.length > 0){//one command mode
      val cmdJson = SocketCommunicationHelper.wrapCommandInJson(cmdArgs)
        SocketCommunicationHelper.writeMsg(cmdJson, out)

        //get the result and print it
        val (resultJson, isStreamClosed) =  SocketCommunicationHelper.readMsg(in)
        if(!isStreamClosed)
          System.out.println(resultJson)
        else{
          System.out.println("No response from server")
        }
      }
      else{//shell mode
        print("kamanja>")
        var userInput = stdIn.readLine
        while ( userInput != null) {
          if(userInput.trim.length > 0) {
            logger.info("MetadataAPIClient - got command {}", userInput)

            val userInputTokens = split(userInput, " ")
            val cmdJson = SocketCommunicationHelper.wrapCommandInJson(userInputTokens)
            SocketCommunicationHelper.writeMsg(cmdJson, out)

            //get the result and print it
            val (resultJson, isStreamClosed) =  SocketCommunicationHelper.readMsg(in)
            if(!isStreamClosed)
              System.out.println(resultJson)
            else{
              //exit???
            }
          }

          print("kamanja>")
          userInput = stdIn.readLine
        }
      }

    } catch {
      case e : UnknownHostException =>
        logger.error("Don't know about host " + hostName, e)
        System.exit(1)
      case e: IOException =>
        logger.error("Couldn't get I/O for the connection to " + hostName, e)
        System.exit(1)

      case e: Throwable =>
        logger.error("Throwable: ", e)
        System.exit(1)
    }
    finally{
      socket.close()
    }
  }
}