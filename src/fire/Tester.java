/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fire;

/**
 *
 * @author nageshbhattu
 */
public class Tester {
    public static void main(String[] args){
        String s= "I, am as great as you";
        System.out.println("The " + s);
        String s1 = s.substring(0, s.indexOf(','));
        s = s.substring(s.indexOf(',')+1);
        
        System.out.println(s1+ " s1 " + s);
    }
}
