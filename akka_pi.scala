import akka.actor._
import akka.routing._

case class Work(startIndex: Int, numberOfElements: Int)
case class Result(result: Double)
case object Calculate

class Worker extends Actor
{
    def calculatePiFor(startIndex: Int, numberOfElements: Int): Double = 
    {
        var result: Double = 0.0
        for (i <- startIndex until (startIndex + numberOfElements))
        {
            if (i%2 == 0)
            {
                result += 1.0/(2*i+1)
            }
            else
            {
                result -= 1.0/(2*i+1)
            }
        }
        return result
    }

    def receive = 
    {
        case Work(startIndex: Int, numberOfElements: Int) =>
        {
            //val pi: Double = this.calculatePiFor(startIndex, numberOfElements)*4.0
            //println("pi = " + pi.toString)
            //context.stop(self)
            val result: Double = this.calculatePiFor(startIndex, numberOfElements)
            sender ! Result(result)
        }

        case _ => println("Worker has received unrecognized message. ")
    }
}

class Master(numberOfWorkers: Int, numberOfMessages: Int, numberOfElements: Int) extends Actor
{
    var pi: Double = 0.0
    var numberOfResults: Int = 0
    val startTime: Long = System.currentTimeMillis
    val workRouter = context.actorOf(Props[Worker].withRouter(RoundRobinPool(numberOfWorkers)), name = "workRouter")

    def receive = 
    {
        case Calculate => 
        {
            for (i <- 0 until numberOfMessages)
            {
                workRouter ! Work(i*numberOfElements, numberOfElements)
            }
        }

        case Result(value) => 
        {
            numberOfResults += 1
            pi += value*4.0
            if (numberOfResults == numberOfMessages)
            {
                println("pi = " + pi.toString)
                val endTime = System.currentTimeMillis
                println("Number of workers: " + numberOfResults)
                println("Total time used in milli-seconds (parallel) = " + (endTime - startTime).toString)
                context.system.shutdown()
            }
        }
    }
}

object test
{
    def calculatePiSequential(upperLimit: Int): Unit = 
    {
        assert(upperLimit > 0)
        var pi: Double = 0.0
        val startTime: Long = System.currentTimeMillis
        for (i <- 0 until upperLimit)
        {
            if (i%2 == 0)
            {
                pi += 1.0/(2*i+1)
            }
            else
            {
                pi -= 1.0/(2*i+1)
            }
        }
        val endTime = System.currentTimeMillis
        println("pi = " + 4*pi)
        println("Total time used in milli-seconds (sequential): " + (endTime - startTime).toString)
    }

    def calculatePiParallel(numberOfWorkers: Int, upperLimit: Int): Unit = 
    {
        val system = ActorSystem("Pi-Calculator")
        val numberOfMessages: Int = numberOfWorkers
        val numberOfElements = upperLimit/numberOfWorkers
        val master = system.actorOf(Props(new Master(numberOfWorkers, numberOfMessages, numberOfElements)), name = "master")
        master ! Calculate
    }

    def main(args: Array[String]): Unit = 
    {
        if (args.length != 1)
        {
            println("numberOfWorkes = args(0). ")
            System.exit(-1)
        }
        val upperLimit: Int = 2000000000
        //calculatePiSequential(upperLimit)
        val numberOfWorkers: Int = args(0).toInt
        calculatePiParallel(numberOfWorkers, upperLimit)
    }
}
