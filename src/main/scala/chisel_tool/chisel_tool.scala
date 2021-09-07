package chisel_tool

import java.io.{FileNotFoundException, IOException}
import javax.annotation.Resource
import scala.io.Source
import java.io._


class clean_code(){
  def aline_the_entire_file_to_this_char(source__path:String, align_to: String){

    try {
      var alignment_index=0
      for (line <- Source.fromFile(source__path).getLines) {

        //println(line)
        var current_index=line.indexOf(align_to)
        if(current_index>alignment_index){
          alignment_index=current_index
        }
      }
      var modifed_IO=""
      for (line<- Source.fromFile(source__path).getLines){
        var IO=line.mkString
        if(IO.length>0){
          var current_index=IO.indexOf(align_to)
          if(current_index>=0){
            var number_of_spaces_to_add=alignment_index-current_index
            var spaces="".toString

            IO=IO.substring(0,current_index-1)+IO.substring(current_index-1,current_index-1).padTo(
              number_of_spaces_to_add,' ')+IO.substring(current_index-1,IO.length)
          }
          modifed_IO=modifed_IO+IO+"\n"
        }
        else {
          modifed_IO=modifed_IO+"\n"
        }

      }
      println(modifed_IO)
      val pw = new PrintWriter(new File(source__path))
      pw.write(modifed_IO)
      pw.close
    } catch {
      case e: FileNotFoundException => println("Couldn't find that file.")
      case e: IOException => println("Got an IOException!")
    }
  }

  def align_to_this_char_from_line_to_line(source__path:String, align_to: String,
                                           frome_line:Int, to_line:Int ){

    try {
      var alignment_index=0
      var text=Source.fromFile(source__path).getLines.toList
      for (line <-frome_line until(to_line) ) {
        var current_index=text(line).indexOf(align_to)
        if(current_index>=alignment_index){
          alignment_index=current_index
        }
      }
      var modifed_IO=""
      var idx=0
      for (line<- Source.fromFile(source__path).getLines){
        var IO=line.mkString
        if(IO.length>0){
          var current_index=IO.indexOf(align_to)
          if(current_index>=0){
            if((idx>=frome_line-1)&(idx<=to_line-1)){
              var number_of_spaces_to_add=alignment_index-current_index
              var spaces="".toString

              IO=IO.substring(0,current_index)+IO.substring(current_index,current_index).padTo(
                number_of_spaces_to_add,' ')+IO.substring(current_index,IO.length)
            }
          }
          modifed_IO=modifed_IO+IO+"\n"
        }
        else {
          modifed_IO=modifed_IO+"\n"
        }
        idx=idx+1
      }
      val pw = new PrintWriter(new File(source__path))
      pw.write(modifed_IO)
      pw.close
    } catch {
      case e: FileNotFoundException => println("Couldn't find that file.")
      case e: IOException => println("Got an IOException!")
    }
  }

  def aline_to_this_char_at_this_point(source__path:String,align_char:String,
                                       align_to_y_coordinate:Int,
                                       frome_line:Int, to_line:Int ){

    try {
      var text=Source.fromFile(source__path).getLines.toList

      var modifed_IO=""
      var idx=0
      for (line<- Source.fromFile(source__path).getLines){
        var IO=line.mkString
        if(IO.length>0){
          var current_index=IO.indexOf(align_char)
          if(current_index>=0){
            if((idx>=frome_line)&(idx<=to_line)){
              var number_of_spaces_to_add=align_to_y_coordinate-current_index
              var spaces="".toString

              IO=IO.substring(0,current_index-1)+IO.substring(current_index-1,current_index-1).padTo(
                number_of_spaces_to_add,' ')+IO.substring(current_index-1,IO.length)
            }
          }
          modifed_IO=modifed_IO+IO+"\n"
        }
        else {
          modifed_IO=modifed_IO+"\n"
        }
        idx=idx+1
      }
      val pw = new PrintWriter(new File(source__path))
      pw.write(modifed_IO)
      pw.close
    } catch {
      case e: FileNotFoundException => println("Couldn't find that file.")
      case e: IOException => println("Got an IOException!")
    }
  }
}