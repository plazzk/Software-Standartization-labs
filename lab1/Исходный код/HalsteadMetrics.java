import java.util.*;

/**
 * Расчёт 6 базовых и 3 расширенных (производных) метрик Холстеда
 * на основе списка токенов, полученного от CSharpTokenizer.
 */
public class HalsteadMetrics {

    public final LinkedHashMap<String, Integer> operatorFreq = new LinkedHashMap<>();
    public final LinkedHashMap<String, Integer> operandFreq = new LinkedHashMap<>();

    // 6 базовых метрик
    public int eta1;       // словарь операторов
    public int eta2;       // словарь операндов
    public int N1;         // общее число операторов
    public int N2;         // общее число операндов
    // (f1j и f2i хранятся в operatorFreq / operandFreq)

    // 3 расширенные (производные) метрики
    public int vocabulary; // eta = eta1 + eta2
    public int length;     // N = N1 + N2
    public double volume;  // V = N * log2(eta)

    public static HalsteadMetrics compute(List<CSharpTokenizer.Token> tokens) {
        HalsteadMetrics hm = new HalsteadMetrics();

        for (CSharpTokenizer.Token tok : tokens) {
            if (tok.type == CSharpTokenizer.TokenType.OPERATOR) {
                hm.operatorFreq.merge(tok.text, 1, Integer::sum);
                hm.N1++;
            } else {
                hm.operandFreq.merge(tok.text, 1, Integer::sum);
                hm.N2++;
            }
        }

        hm.eta1 = hm.operatorFreq.size();
        hm.eta2 = hm.operandFreq.size();
        hm.vocabulary = hm.eta1 + hm.eta2;
        hm.length = hm.N1 + hm.N2;
        hm.volume = hm.vocabulary > 0
                ? hm.length * (Math.log(hm.vocabulary) / Math.log(2))
                : 0.0;
        return hm;
    }
}
