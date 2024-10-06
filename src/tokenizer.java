import java.util.*;

public class tokenizer {
    List<String> line  ;

    public int pos;
    public Tokenizer(List<String> text){
        pos = 0 ;
        List<String> temp = new ArrayList<>();
        for (String inputString : text) {
//            List<String> tokens1 = tokenize(inputString);
            temp.addAll(tokens1);
        }
        this.line = temp;
    }
}
