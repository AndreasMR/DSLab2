package controller.user;

import java.util.Comparator;

public class UserNameComparator implements Comparator<UserInfo> {
	
	@Override
	public int compare(UserInfo o1, UserInfo o2) {
		return o1.getName().compareTo(o2.getName());
	}
}
