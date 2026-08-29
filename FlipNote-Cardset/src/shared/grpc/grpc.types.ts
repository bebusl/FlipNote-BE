import { Observable } from 'rxjs';

// ── cardset.proto 메시지 타입 ─────────────────────────────────────

export interface IsCardSetViewableRequest {
  cardSetId: number;
  userId: number;
}

export interface IsCardSetViewableResponse {
  viewable: boolean;
}

export interface GetCardSetsByIdsRequest {
  cardSetIds: number[];
  userId: number;
}

export interface CardSetSummary {
  id: number;
  name: string;
  groupId: number;
  visibility: string;
  category: string;
  hashtag: string;
  imageRefId: number | null;
  cardCount: number;
}

export interface GetCardSetsByIdsResponse {
  cardSets: CardSetSummary[];
}

// ── 서비스 클라이언트 인터페이스 ────────────────────────────────────
// 다른 서비스에서 이 서버를 호출할 때 사용하는 클라이언트 타입

export interface CardsetServiceClient {
  isCardSetViewable(
    data: IsCardSetViewableRequest,
  ): Observable<IsCardSetViewableResponse>;
  getCardSetsByIds(
    data: GetCardSetsByIdsRequest,
  ): Observable<GetCardSetsByIdsResponse>;
}
