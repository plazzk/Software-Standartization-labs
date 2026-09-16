using System;
using System.Collections.Generic;

namespace HalsteadDemo
{
    class Program
    {
        const double Eps = 0.0001;

        static void Main(string[] args) {
            Console.WriteLine("Введите число X:");
            double x = double.Parse(Console.ReadLine());
            double sinValue = SinSeries(x, Eps);
            Console.WriteLine("sin(x) = " + sinValue);
            int[] numbers = { 4, 8, 15, 16, 23, 42 };
            int sum = SumArray(numbers);
            double avg = (double)sum / numbers.Length;
            Console.WriteLine("Сумма = " + sum + ", среднее = " + avg);
            List<int> evens = new List<int>();
            foreach (int n in numbers) {
                if (n % 2 == 0) {
                    evens.Add(n);
                }
            }
            Console.WriteLine("Кол-во чётных: " + evens.Count);
            int k = 0;
            while (k < numbers.Length) {
                Console.WriteLine("numbers[" + k + "] = " + numbers[k]);
                k++;
            }
            for (int j = 0; j < 5; j++) {
                int fact = Factorial(j);
                Console.WriteLine(j + "! = " + fact);
            }
            string grade = ClassifyScore(87);
            Console.WriteLine("Оценка: " + grade);
            try {
                int result = SafeDivide(10, 0);
                Console.WriteLine("Результат деления: " + result);
            } catch (DivideByZeroException ex) {
                Console.WriteLine("Ошибка: " + ex.Message);
            } finally {
                Console.WriteLine("Вычисления завершены.");
            }
            bool flag = (sum > 50) && (avg < 20) || !(k == 3);
            Console.WriteLine("Флаг: " + flag);
            int max = numbers[0] > numbers[1] ? numbers[0] : numbers[1];
            Console.WriteLine("Максимум первых двух: " + max);
            Console.WriteLine("Работа программы завершена.");
        }

        static double SinSeries(double x, double eps) {
            double y = x;
            double term = x;
            int n = 1;
            do {
                term = -term * x * x / ((2 * n) * (2 * n + 1));
                y += term;
                n++;
            } while (Math.Abs(term) >= eps);
            return y;
        }

        static int SumArray(int[] arr) {
            int total = 0;
            for (int idx = 0; idx < arr.Length; idx++) {
                total += arr[idx];
            }
            return total;
        }

        static int Factorial(int n) {
            if (n <= 1) {
                return 1;
            }
            return n * Factorial(n - 1);
        }

        static string ClassifyScore(int score) {
            switch (score / 10) {
                case 10:
                case 9:
                    return "Отлично";
                case 8:
                case 7:
                    return "Хорошо";
                case 6:
                    return "Удовлетворительно";
                default:
                    return "Неудовлетворительно";
            }
        }

        static int SafeDivide(int a, int b) {
            if (b == 0) {
                throw new DivideByZeroException("Деление на ноль недопустимо");
            }
            return a / b;
        }
    }
}
