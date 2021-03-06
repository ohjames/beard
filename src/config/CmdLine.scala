// Copyright (c) 2009-2010 James Pike
// Released under the MIT license
package org.beard.config
import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue

class UnknownOption(unknownKey:String) extends Throwable {
    val key = unknownKey
}

class CmdLineIterator(args:Array[String]) {
    var idx = 0
    var position = 0
    var value = if (args.size > 0) args(0) else ""

    def atEnd = idx >= args.size
    def +=(inc:Int) {
        idx += inc
        value = if (args.size > idx) args(idx) else ""
    }
    def size = value.size - position
    def front = value(position)
    def positionedValue = value.substring(position)
}

class CmdLine {
    private var values = new HashMap[String, ValueAbstract]
    private var descriptions = new Queue[HelpDescription]
    var positionals = new Queue[String]

    private class HelpDescription(aKey:String, aDescription:String, aAlternates:List[String], aSuffix:String) {
        val key = aKey
        val description = aDescription
        var alternates:List[String] = aAlternates
        var suffix = aSuffix

        def headerSize:Int = {
            if (alternates == Nil) keySize(key) + suffix.size
            else (2 * (alternates.size - 1)) + suffix.size +
                aAlternates.foldLeft(keySize(key) + 5) { _ + keySize(_) }
        }

        private def keySize(key:String) =
            if (key.size > 1) 2 + key.size else 1 + key.size

        private def keyString(key:String) =
            if (key.size > 1) "--" + key else "-" + key

        def header:String = {
            if (alternates == Nil) "  " + keyString(key) + suffix
            else
                "  " + keyString(key) + " [ " +
                    alternates.map(keyString(_)).reduceLeft(_ + ", " + _) + " ]" + suffix
        }
    }

    def +=(keys:String, value:ValueAbstract, desc:String):CmdLine = {
        values += keys -> value
        descriptions += new HelpDescription(keys, desc, Nil, value.suffix)
        return this
    }

    def +=(keys:(String, String), value:ValueAbstract, desc:String):CmdLine = {
        values += keys._1 -> value
        values += keys._2 -> value
        descriptions += new HelpDescription(keys._1, desc, keys._2 :: Nil, value.suffix)
        return this
    }

    def +=(keys:(String, String, String), value:ValueAbstract, desc:String):CmdLine = {
        values += keys._1 -> value
        values += keys._2 -> value
        values += keys._3 -> value
        descriptions += new HelpDescription(keys._1, desc, keys._2 :: keys._3 :: Nil, value.suffix)
        return this
    }

    def help {
        // + 2 for header and + 2 for space after argument header
        val maxLength = Iterable.max(descriptions.map( _.headerSize )) + 4
        descriptions.foreach(desc => {
            val thisHeader = desc.header
            print(thisHeader +  " " * (maxLength - thisHeader.size))
            println(desc.description)
        })
    }

    def parse(args:Array[String]) {
        var it = new CmdLineIterator(args)
        while (! it.atEnd) {
            if (it.value.size > 1 && it.value(0) == '-') {
                if (it.value(1) == '-') {
                    if (it.size == 2) {
                        it += 1
                        while (! it.atEnd) {
                            positionals += it.value
                            it += 1
                        }
                    }
                    else {
                        val search = it.value.substring(2)
                        if (values.contains(search)) {
                            it += 1
                            values(search)(it)
                        }
                        else throw new UnknownOption(search)
                    }
                }
                else {
                    it.position = 1
                    var good = false
                    do {
                        val search = it.front.toString
                        if (values.contains(search)) {
                            if (it.size == 1) {
                                it += 1
                                it.position = 0
                                values(search)(it)
                                good = false
                            }
                            else {
                                it.position += 1
                                good = values(search).after(it)
                            }
                        }
                        else throw new UnknownOption(search)
                    } while (good)
                }
            }
            else positionals += it.value
        }
    }
}
