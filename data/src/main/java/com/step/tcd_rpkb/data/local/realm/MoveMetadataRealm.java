package com.step.tcd_rpkb.data.local.realm;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class MoveMetadataRealm extends RealmObject {
    @PrimaryKey
    private String moveUuid;
    private String moveNumber;
    private String moveDate;
    @Index
    private String signingStatus;
    private boolean isSyncedWith1C;
    private long createdTimestamp;
    private long lastModifiedTimestamp;

    public MoveMetadataRealm() { }

    public String getMoveUuid() {
        return moveUuid;
    }

    public void setMoveUuid(String moveUuid) {
        this.moveUuid = moveUuid;
    }

    public String getMoveNumber() {
        return moveNumber;
    }

    public void setMoveNumber(String moveNumber) {
        this.moveNumber = moveNumber;
    }

    public String getMoveDate() {
        return moveDate;
    }

    public void setMoveDate(String moveDate) {
        this.moveDate = moveDate;
    }

    public String getSigningStatus() {
        return signingStatus;
    }

    public void setSigningStatus(String signingStatus) {
        this.signingStatus = signingStatus;
    }

    public boolean isSyncedWith1C() {
        return isSyncedWith1C;
    }

    public void setSyncedWith1C(boolean syncedWith1C) {
        isSyncedWith1C = syncedWith1C;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }
}
