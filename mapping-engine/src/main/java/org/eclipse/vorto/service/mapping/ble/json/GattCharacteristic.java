package org.eclipse.vorto.service.mapping.ble.json;

import java.util.Arrays;

public class GattCharacteristic {

	private String uuid;
	private byte[] data;
	
	protected GattCharacteristic() {	
	}
	
	public GattCharacteristic(String uuid, byte[] data) {
		this.uuid = uuid;
		this.data = data;
	}
	
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(data);
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GattCharacteristic other = (GattCharacteristic) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
	
	

}
