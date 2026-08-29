export class CardsetManager {
  constructor(
    public readonly id: number,
    public readonly userId: number,
    public readonly cardSetId: number,
  ) {}

  static create(props: { userId: number; cardSetId: number }): CardsetManager {
    return new CardsetManager(0, props.userId, props.cardSetId);
  }
}
