import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import com.google.common.collect.ImmutableSet;

import utils.FileUtils;

public class SnailMath {
  public static final Set<Character> DIGIT_SET = ImmutableSet.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');

  public static void main(String[] args) {
    List<String> input = FileUtils.readFile(SnailMath.class, "test-18.txt");

    List<SnailNumber> snailNumbersPart1 = new ArrayList<>();
    List<SnailNumber> snailNumbersPart2 = new ArrayList<>();
    for (String line : input) {
      snailNumbersPart1.add(parseSnailNumber(line));
      snailNumbersPart2.add(parseSnailNumber(line));
    }

    snailNumbersPart1.forEach(System.out::println);

    SnailNumber currNumber = snailNumbersPart1.get(0);
    for (int i = 1; i < snailNumbersPart1.size(); i++) {
      currNumber = addSnailNumbers(currNumber, snailNumbersPart1.get(i));
      currNumber.reduce();
    }

    System.out.println(currNumber);
    System.out.println(currNumber.magnitude());

    List<SnailNumber[]> duos = new ArrayList<>();
    for (int i = 0; i < snailNumbersPart2.size(); i++) {
      for (int j = 1; j < snailNumbersPart2.size(); j++) {
        if (i == j) continue;
        duos.add(new SnailNumber[]{snailNumbersPart2.get(i).deepCopy(), snailNumbersPart2.get(j).deepCopy()});
      }
    }

    long max = Long.MIN_VALUE;
    for (SnailNumber[] duo : duos) {
      currNumber = addSnailNumbers(duo[0], duo[1]);
      currNumber.reduce();

      long magnitude = currNumber.magnitude();
      if (magnitude > max) {
        max = magnitude;
      }
    }

    System.out.println(max);
  }

  private static SnailNumber parseSnailNumber(String line) {
    Stack<SnailNumber> snailNumbers = new Stack<>();
    int openBrackets = 0;
    for (char c : line.toCharArray()) {
      if (c == '[') {
        openBrackets++;
        snailNumbers.push(new SnailNumber(openBrackets));
      } else if (DIGIT_SET.contains(c)) {
        snailNumbers.push(new SnailNumber(openBrackets, c - '0'));
      } else if (c == ']') {
        SnailNumber snailNumber2 = snailNumbers.pop();
        SnailNumber snailNumber1 = snailNumbers.pop();
        SnailNumber snailNumber0 = snailNumbers.peek();

        snailNumber2.rightValue = true;
        snailNumber2.parent = snailNumber0;
        snailNumber1.parent = snailNumber0;

        snailNumber0.setNumbers(snailNumber1, snailNumber2);
        openBrackets--;
      }

    }

    return snailNumbers.pop();
  }

  public static SnailNumber addSnailNumbers(SnailNumber x, SnailNumber y) {
    SnailNumber sum = new SnailNumber(1, x, y);
    x.parent = sum;
    y.parent = sum;
    x.increaseDepth();
    y.increaseDepth();

    return sum;
  }


  public static class SnailNumber {
    int depth;
    long value;
    SnailNumber[] snailNumber;
    SnailNumber parent = null;
    boolean rightValue;


    public SnailNumber(int depth) {
      this.depth = depth;
      value = -1;
      snailNumber = new SnailNumber[2];
    }

    public SnailNumber(int depth, long value) {
      this.depth = depth;
      this.value = value;
      snailNumber = new SnailNumber[]{};
    }

    public SnailNumber(SnailNumber parent, int depth, long value) {
      this.depth = depth;
      this.value = value;
      this.parent = parent;
      snailNumber = new SnailNumber[]{};
    }

    public SnailNumber(int depth, SnailNumber x, SnailNumber y) {
      this.depth = depth;
      this.snailNumber = new SnailNumber[]{x, y};
      this.value = -1;
    }

    public void setNumbers(SnailNumber x, SnailNumber y) {
      this.snailNumber[0] = x;
      this.snailNumber[1] = y;
    }

    private void increaseDepth() {
      Queue<SnailNumber> snailNumbers = new ArrayDeque<>();
      snailNumbers.add(this);
      while (!snailNumbers.isEmpty()) {
        SnailNumber snailNumber = snailNumbers.poll();
        snailNumber.depth = (snailNumber.parent == null) ? 1 : snailNumber.parent.depth + 1;

        if (snailNumber.value == -1) {
          snailNumbers.add(snailNumber.snailNumber[0]);
          snailNumbers.add(snailNumber.snailNumber[1]);
        }
      }
    }

    public void reduce() {
      boolean explodingOrSplitting = true;
      while (explodingOrSplitting) {
        List<SnailNumber> needToExplode = this.getCompoundNumbersAtDepth(5);
        explodingOrSplitting = needToExplode.stream().findFirst().map(SnailNumber::explode).orElse(false);
        if (!explodingOrSplitting) {
          explodingOrSplitting = this.getLeavesLeftFirst().stream().filter(n -> n.value > 9).findFirst().map(SnailNumber::split).orElse(false);
        }
      }
    }

    public boolean split() {
      if (this.value < 10) {
        return false;
      }

      long value = this.value;
      if (value % 2 == 0) {
        this.snailNumber = new SnailNumber[]{new SnailNumber(this, depth + 1, value / 2), new SnailNumber(this, depth + 1, value / 2)};
      } else {
        this.snailNumber = new SnailNumber[]{new SnailNumber(this, depth + 1, value / 2), new SnailNumber(this, depth + 1, (value / 2) + 1)};
      }
      this.value = -1;
      return true;
    }

    public boolean explode() {
      SnailNumber rightNumber = snailNumber[1];
      SnailNumber leftNumber = snailNumber[0];

      SnailNumber lastNode = this;
      SnailNumber currNode = this.parent;
      boolean rightExploded = false, leftExploded = false;
      while (currNode != null && (!leftExploded || !rightExploded)) {
        SnailNumber rightSibling = currNode.snailNumber[1];
        SnailNumber leftSibling = currNode.snailNumber[0];

        if (!rightExploded && rightSibling != null && lastNode != rightSibling) {
          SnailNumber child = rightSibling;
          while (child != null) {
            if (child.value != -1) {
              child.value += rightNumber.value;
              rightExploded = true;
              break;
            } else {
              child = child.snailNumber[0];
            }
          }
        }

        if (!leftExploded && leftSibling != null && lastNode != leftSibling) {
          SnailNumber child = leftSibling;
          while (child != null) {
            if (child.value != -1) {
              child.value += leftNumber.value;
              leftExploded = true;
              break;
            } else {
              child = child.snailNumber[1];
            }
          }
        }

        lastNode = currNode;
        currNode = currNode.parent;
      }

      this.value = 0;
      this.snailNumber[0] = null;
      this.snailNumber[1] = null;
      return leftExploded || rightExploded;
    }

    @Override
    public String toString() {
      if (value != -1) {
        return "" + value;
      } else {
        return Arrays.toString(snailNumber);
      }
    }

    public List<SnailNumber> getCompoundNumbersAtDepth(int givenDepth) {
      Stack<SnailNumber> snailNumbers = new Stack<>();
      List<SnailNumber> numbersAtDepth = new ArrayList<>();
      snailNumbers.add(this);
      while (!snailNumbers.isEmpty()) {
        SnailNumber snailNumber = snailNumbers.pop();
        if (snailNumber.depth == givenDepth && snailNumber.value == -1 &&
            snailNumber.snailNumber[0].value != -1 && snailNumber.snailNumber[1].value != -1) {
          numbersAtDepth.add(snailNumber);
        }

        if (snailNumber.value == -1) {
          snailNumbers.push(snailNumber.snailNumber[1]);
          snailNumbers.push(snailNumber.snailNumber[0]);
        }
      }
      return numbersAtDepth;
    }

    public List<SnailNumber> getLeavesLeftFirst() {
      Stack<SnailNumber> snailNumbers = new Stack<>();
      List<SnailNumber> leaves = new ArrayList<>();
      snailNumbers.add(this);
      while (!snailNumbers.isEmpty()) {
        SnailNumber snailNumber = snailNumbers.pop();
        if (snailNumber.value != -1) {
          leaves.add(snailNumber);
        }

        if (snailNumber.value == -1) {
          snailNumbers.add(snailNumber.snailNumber[1]);
          snailNumbers.add(snailNumber.snailNumber[0]);
        }
      }
      return leaves;
    }

    public long magnitude() {
      Stack<SnailNumber> snailNumbers = new Stack<>();
      snailNumbers.add(this);
      while (!snailNumbers.isEmpty()) {
        SnailNumber snailNumber = snailNumbers.pop();

        if (snailNumber.value == -1 && snailNumber.snailNumber[0].value != -1 && snailNumber.snailNumber[1].value != -1) {
          snailNumber.value = snailNumber.snailNumber[0].value * 3 + snailNumber.snailNumber[1].value * 2;
        }

        if (snailNumber.value == -1) {
          snailNumbers.add(snailNumber);
          snailNumbers.add(snailNumber.snailNumber[1]);
          snailNumbers.add(snailNumber.snailNumber[0]);

        }
      }

      return this.value;
    }

    public SnailNumber deepCopy() {
      return parseSnailNumber(this.toString().replaceAll(" ", ""));
    }
  }
}

