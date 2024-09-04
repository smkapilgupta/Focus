package com.example.focus.common

import java.util.UUID


class Utilities {
    companion object {
        public fun formatTimeFromSeconds(timeSeconds: Long): String {
            val minutes = timeSeconds / 60;
            val seconds = timeSeconds % 60;

            return (if (minutes<10) "0".plus(minutes) else minutes.toString()).plus(" : ").plus(if (seconds<10) "0".plus(seconds) else seconds.toString());
        }

        public fun isInsidePocket(orientation: FloatArray,lightSensorReading: Float,proximitySensorReading: Float):Boolean{
            val res=isSomethingCoveringScreen(lightSensorReading,proximitySensorReading)
//            println("isInsidePocket: "+res);
            return res;
        }
        public fun isMoving(accelerometerReading: FloatArray):Boolean{
            val Ax: Float = accelerometerReading.get(0)
            val Ay: Float = accelerometerReading.get(1)
            val Az: Float = accelerometerReading.get(2)

            val normsqA = Ax * Ax + Ay * Ay + Az * Az
            val g = 9.81f
            val freeFallGravitySquared = g * g

            return Math.abs(normsqA-freeFallGravitySquared)>1
        }
        public fun isOnHorizontalSurface(orientation: FloatArray):Boolean{
            val res=Math.abs(orientation.get(2))>172||return Math.abs(orientation.get(2))<8
//            println("isOnHorizontalSurface: "+res);
            return res;
        }
        public fun isFacingDown(orientation: FloatArray):Boolean{
            val res=Math.abs(orientation.get(2))>100
//            println("isFacingDown: "+res);
            return res
        }
        public fun isFacingUp(orientation: FloatArray):Boolean{
            return Math.abs(orientation.get(2))<30;
        }
        public fun isVertical(accelerometer:Array<Float>):Boolean{
            return false;
        }
        public fun isDark(lightSensorReading: Float):Boolean{
            val res=lightSensorReading<10
//            println("isDark: "+res);
            return res
        }

        public fun isSomethingInProximity(proximitySensorReading:Float):Boolean{
            val res=proximitySensorReading<5
//            println("isSomethingInProximity: "+res);
            return res
        }

        public fun isSomethingCoveringScreen(lightSensorReading:Float, proximitySensorReading:Float):Boolean{
            val res=isDark(lightSensorReading) && isSomethingInProximity(proximitySensorReading)
//            println("isSomethingCoveringScreen: "+res);
            return res;
        }
        public fun convertRadianOrientationToDegree(orientation:FloatArray){
            orientation.set(0, (orientation.get(0)*180/Constants.pi).toFloat());
            orientation.set(1, (orientation.get(1)*180/Constants.pi).toFloat());
            orientation.set(2, (orientation.get(2)*180/Constants.pi).toFloat());
        }

        var lastRN:Long=0;
        fun generateRandomNumber(rangeStart:Long,rangeEnd:Long):Long{
            var uuid= UUID.randomUUID().toString()
            var sumOfChars=0;
            for(i:Int in 0..uuid.length-1){
                sumOfChars+=uuid.get(i).code
            }
            var newRN:Long= rangeStart+(lastRN+sumOfChars)%(rangeEnd-rangeStart+1)
            if(newRN == lastRN)
                newRN=rangeStart+(lastRN+sumOfChars+1)%(rangeEnd-rangeStart+1)
            lastRN=newRN
            return newRN
        }
    }
}